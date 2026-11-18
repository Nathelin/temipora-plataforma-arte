#!/usr/bin/env node
/**
 * Genera los diagramas de clases del núcleo de Temipora a partir del código Java.
 * Sin dependencias: lexer + parser de declaraciones suficiente para este proyecto.
 *
 * Uso:
 *   node generar.mjs            -> escribe los .mmd y modelo.json
 *   node generar.mjs --render   -> además renderiza SVG/PNG con mermaid-cli
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { execSync } from "node:child_process";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../../..");
const SRC = path.join(ROOT, "backend-springboot/src/main/java");
const OUT = HERE;

const MODIFIERS = new Set([
  "public", "private", "protected", "static", "final", "abstract",
  "sealed", "non-sealed", "strictfp", "transient", "volatile", "native",
  "synchronized", "default",
]);

const JPA_RELATIONS = new Set([
  "ManyToOne", "OneToMany", "OneToOne", "ManyToMany", "ElementCollection",
]);

const EXTERNAL_INTERFACES = new Set([
  "JpaRepository", "JpaSpecificationExecutor", "CrudRepository", "UserDetailsService",
]);

/* ------------------------------------------------------------------ lexer */

function lex(src) {
  const tokens = [];
  let i = 0;
  const n = src.length;
  const isIdStart = (c) => /[A-Za-z_$]/.test(c);
  const isIdPart = (c) => /[A-Za-z0-9_$]/.test(c);

  while (i < n) {
    const c = src[i];

    if (c === " " || c === "\t" || c === "\r" || c === "\n" || c === "\f" || c === "\v") {
      i++;
      continue;
    }
    if (c === "/" && src[i + 1] === "/") {
      while (i < n && src[i] !== "\n") i++;
      continue;
    }
    if (c === "/" && src[i + 1] === "*") {
      i += 2;
      while (i < n && !(src[i] === "*" && src[i + 1] === "/")) i++;
      i += 2;
      continue;
    }
    if (c === '"' && src[i + 1] === '"' && src[i + 2] === '"') {
      const start = i;
      i += 3;
      while (i < n && !(src[i] === '"' && src[i + 1] === '"' && src[i + 2] === '"')) i++;
      i += 3;
      tokens.push({ t: "string", v: src.slice(start, i), s: start, e: i });
      continue;
    }
    if (c === '"' || c === "'") {
      const q = c;
      const start = i;
      i++;
      while (i < n && src[i] !== q) {
        if (src[i] === "\\") i++;
        i++;
      }
      i++;
      tokens.push({ t: "string", v: src.slice(start, i), s: start, e: i });
      continue;
    }
    if (isIdStart(c)) {
      const start = i;
      while (i < n && isIdPart(src[i])) i++;
      tokens.push({ t: "id", v: src.slice(start, i), s: start, e: i });
      continue;
    }
    if (/[0-9]/.test(c)) {
      const start = i;
      while (i < n && /[0-9A-Fa-fxXlL._]/.test(src[i])) i++;
      tokens.push({ t: "num", v: src.slice(start, i), s: start, e: i });
      continue;
    }
    token: {
      for (const multi of ["...", "->", "::", ">=", "<=", "!=", "==", "&&", "||", "++", "--"]) {
        if (src.startsWith(multi, i)) {
          tokens.push({ t: "punct", v: multi, s: i, e: i + multi.length });
          i += multi.length;
          break token;
        }
      }
      tokens.push({ t: "punct", v: c, s: i, e: i + 1 });
      i++;
    }
  }
  return tokens;
}

/* ------------------------------------------------------------- navigation */

function matchBrace(tokens, i, limit = tokens.length) {
  let depth = 0;
  for (let k = i; k < limit; k++) {
    if (tokens[k].v === "{") depth++;
    else if (tokens[k].v === "}") {
      depth--;
      if (depth === 0) return k;
    }
  }
  return limit - 1;
}

function matchParen(tokens, i, limit = tokens.length) {
  let depth = 0;
  for (let k = i; k < limit; k++) {
    if (tokens[k].v === "(") depth++;
    else if (tokens[k].v === ")") {
      depth--;
      if (depth === 0) return k;
    }
  }
  return limit - 1;
}

function skipGeneric(tokens, i, limit = tokens.length) {
  let depth = 0;
  for (let k = i; k < limit; k++) {
    const v = tokens[k].v;
    if (v === "<") depth++;
    else if (v === ">") {
      depth--;
      if (depth === 0) return k + 1;
    } else if (v === "{" || v === ";") return k;
  }
  return limit;
}

function splitSpans(tokens, start, end, sep) {
  const spans = [];
  let depth = 0;
  let from = start;
  for (let k = start; k < end; k++) {
    const v = tokens[k].v;
    if (v === "<" || v === "(" || v === "[" || v === "{") depth++;
    else if (v === ">" || v === ")" || v === "]" || v === "}") depth--;
    else if (v === sep && depth === 0) {
      spans.push([from, k]);
      from = k + 1;
    }
  }
  if (from < end) spans.push([from, end]);
  return spans;
}

/* ------------------------------------------------------------- annotations */

function readAnnotationName(tokens, i) {
  // tokens[i] === '@'; returns { name, next }
  let p = i + 1;
  const parts = [];
  while (p < tokens.length && tokens[p].t === "id") {
    parts.push(tokens[p].v);
    if (tokens[p + 1]?.v === ".") p += 2;
    else { p++; break; }
  }
  return { name: parts[parts.length - 1] ?? "", next: p };
}

function readAnnotations(tokens, i) {
  const anns = [];
  let p = i;
  while (tokens[p]?.v === "@") {
    const { name, next } = readAnnotationName(tokens, p);
    if (!name || name === "interface") break;
    p = next;
    let args = null;
    if (tokens[p]?.v === "(") {
      const close = matchParen(tokens, p);
      args = tokens.slice(p + 1, close).map((t) => t.v).join(" ");
      p = close + 1;
    }
    anns.push({ name, args });
  }
  return { anns, p };
}

/* ------------------------------------------------------------------ types */

function renderType(tokens) {
  let s = "";
  for (const t of tokens) {
    const v = t.v;
    if (v === "." || v === "<" || v === ">" || v === "[" || v === "]" || v === ",") {
      s += v;
    } else if (t.t === "id") {
      const needsSpace = s.length > 0 && !/[.<,\[]$/.test(s);
      s += (needsSpace ? " " : "") + v;
    } else {
      s += v;
    }
  }
  return s.replace(/\s+/g, " ").trim();
}

function simpleTypeName(type) {
  if (!type) return null;
  const clean = type.replace(/\?/g, "").replace(/\bextends\b/g, "").replace(/\bsuper\b/g, "").trim();
  const lt = clean.indexOf("<");
  const outer = (lt >= 0 ? clean.slice(0, lt) : clean).replace(/\[\]/g, "").trim();
  const outerName = outer.split(".").pop();
  if (lt >= 0) {
    const inner = clean.slice(lt + 1, clean.lastIndexOf(">"));
    const innerSimple = (inner.split(",")[0] || "").replace(/\[\]/g, "").trim().split(".").pop();
    if (["List", "Set", "Collection", "Iterable", "ArrayList", "HashSet", "LinkedHashSet", "Optional"].includes(outerName)) {
      return innerSimple || outerName;
    }
  }
  return outerName;
}

function readTypeAt(tokens, i, end) {
  // tokens[i] debe ser id; devuelve { name, args, next }
  const start = i;
  let p = i;
  const parts = [];
  while (p < end && tokens[p].t === "id") {
    parts.push(tokens[p].v);
    if (tokens[p + 1]?.v === ".") p += 2;
    else { p++; break; }
  }
  const name = parts[parts.length - 1] ?? "";
  const args = [];
  if (tokens[p]?.v === "<") {
    const after = skipGeneric(tokens, p, end);
    const innerEnd = tokens[after - 1]?.v === ">" ? after - 1 : after;
    for (const [a, b] of splitSpans(tokens, p + 1, innerEnd, ",")) {
      if (a < b && tokens[a].t === "id") args.push(readTypeAt(tokens, a, b).name);
    }
    p = after;
  }
  return { name, args, raw: renderType(tokens.slice(start, p)), next: p };
}

/* ----------------------------------------------------------------- parser */

function parseJava(src, file) {
  const tokens = lex(src);
  if (!tokens.length) return null;

  let i = 0;
  let pkg = "";
  if (tokens[0].v === "package") {
    i = 1;
    const parts = [];
    while (i < tokens.length && tokens[i].v !== ";") {
      if (tokens[i].t === "id") parts.push(tokens[i].v);
      i++;
    }
    pkg = parts.join(".");
    i++;
  }

  let depth = 0;
  let decl = -1;
  for (; i < tokens.length; i++) {
    const v = tokens[i].v;
    if (v === "{") depth++;
    else if (v === "}") depth--;
    else if (
      depth === 0 &&
      (v === "class" || v === "interface" || v === "enum" || v === "record") &&
      tokens[i + 1]?.t === "id" &&
      tokens[i - 1]?.v !== "@"
    ) {
      decl = i;
      break;
    }
  }
  if (decl < 0) return null;

  const kind = tokens[decl].v;
  const name = tokens[decl + 1].v;

  // anotaciones hacia atrás (saltando modificadores)
  const anns = [];
  let back = decl - 1;
  while (back >= 0) {
    const v = tokens[back].v;
    if (MODIFIERS.has(v)) { back--; continue; }
    if (v === ")") {
      let d = 0;
      let k = back;
      for (; k >= 0; k--) {
        if (tokens[k].v === ")") d++;
        else if (tokens[k].v === "(") { d--; if (d === 0) break; }
      }
      const nameTok = tokens[k - 1];
      const atTok = tokens[k - 2];
      if (nameTok?.t === "id" && atTok?.v === "@") {
        anns.unshift({ name: nameTok.v, args: src.slice(tokens[k].e, tokens[back].s) });
        back = k - 3;
        continue;
      }
      break;
    }
    if (tokens[back].t === "id" && tokens[back - 1]?.v === "@") {
      anns.unshift({ name: tokens[back].v, args: null });
      back -= 2;
      continue;
    }
    break;
  }

  let h = decl + 2;
  if (tokens[h]?.v === "<") h = skipGeneric(tokens, h);

  const extendsList = [];
  const implementsList = [];
  while (h < tokens.length && tokens[h].v !== "{") {
    if (tokens[h].v === "extends" || tokens[h].v === "implements") {
      const target = tokens[h].v === "implements" ? implementsList : extendsList;
      h++;
      while (h < tokens.length && tokens[h].v !== "{" &&
             tokens[h].v !== "extends" && tokens[h].v !== "implements") {
        if (tokens[h].t === "id") {
          const t = readTypeAt(tokens, h, tokens.length);
          target.push(t);
          h = t.next;
        } else h++;
      }
      continue;
    }
    h++;
  }
  if (tokens[h]?.v !== "{") return null;
  const bodyStart = h;
  const bodyEnd = matchBrace(tokens, bodyStart);

  const cls = {
    name, kind, pkg, file,
    annotations: anns,
    extends: extendsList,
    implements: implementsList,
    fields: [],
    ctorParams: [],
    methods: [],
    enumConstants: [],
  };

  let p = bodyStart + 1;

  if (kind === "enum") {
    let d = 0;
    let semi = -1;
    for (let k = p; k < bodyEnd; k++) {
      const v = tokens[k].v;
      if (v === "(" || v === "{" || v === "<") d++;
      else if (v === ")" || v === "}" || v === ">") d--;
      else if (v === ";" && d === 0) { semi = k; break; }
    }
    const end = semi >= 0 ? semi : bodyEnd;
    for (const [a, b] of splitSpans(tokens, p, end, ",")) {
      const first = tokens.slice(a, b).find((t, idx, arr) =>
        t.t === "id" && arr[idx - 1]?.v !== "@" && !["new"].includes(t.v));
      if (first) cls.enumConstants.push(first.v);
    }
    p = semi >= 0 ? semi + 1 : bodyEnd;
  }

  while (p < bodyEnd) {
    if (tokens[p].v === ";" || tokens[p].v === ",") { p++; continue; }

    const a = readAnnotations(tokens, p);
    const memberAnns = a.anns;
    p = a.p;

    let visibility = "package";
    let isStatic = false;
    let guard = 0;
    while (p < bodyEnd) {
      const v = tokens[p].v;
      if (v === "@") {
        const a2 = readAnnotations(tokens, p);
        memberAnns.push(...a2.anns);
        p = a2.p;
        continue;
      }
      if (MODIFIERS.has(v)) {
        if (v === "public") visibility = "public";
        else if (v === "private") visibility = "private";
        else if (v === "protected") visibility = "protected";
        else if (v === "static") isStatic = true;
        p++;
        continue;
      }
      break;
    }
    if (++guard > 50) break;
    if (p >= bodyEnd) break;

    const v = tokens[p].v;
    if (v === "class" || v === "interface" || v === "enum" || v === "record") {
      p = skipBlockFrom(tokens, p, bodyEnd);
      continue;
    }
    if (v === "{") { p = matchBrace(tokens, p, bodyEnd) + 1; continue; }
    if (v === ";") { p++; continue; }

    let q = p;
    let angle = 0;
    let paren = -1;
    let assign = -1;
    let semi = -1;
    let brace = -1;
    while (q < bodyEnd) {
      const t = tokens[q].v;
      if (t === "<") angle++;
      else if (t === ">") angle--;
      else if (t === "(" && angle === 0) { paren = q; break; }
      else if (t === "=" && angle === 0) { assign = q; break; }
      else if (t === ";" && angle === 0) { semi = q; break; }
      else if (t === "{" && angle === 0) { brace = q; break; }
      q++;
    }

    if (paren >= 0) {
      const nameIdx = paren - 1;
      const mName = tokens[nameIdx]?.v;
      if (!mName || tokens[nameIdx].t !== "id") { p = paren + 1; continue; }
      const close = matchParen(tokens, paren, bodyEnd);
      const params = parseParams(tokens, paren + 1, close);

      let typeStart = p;
      if (tokens[typeStart]?.v === "<") typeStart = skipGeneric(tokens, typeStart, nameIdx);
      const returnType = renderType(tokens.slice(typeStart, nameIdx));

      if (mName === name) {
        cls.ctorParams.push(...params);
      } else {
        cls.methods.push({
          name: mName,
          params,
          returnType,
          annotations: memberAnns,
          visibility,
          isStatic,
        });
      }

      let r = close + 1;
      while (r < bodyEnd && tokens[r].v !== "{" && tokens[r].v !== ";") r++;
      p = tokens[r]?.v === "{" ? matchBrace(tokens, r, bodyEnd) + 1 : r + 1;
      continue;
    }

    const term = assign >= 0 ? assign : semi >= 0 ? semi : brace >= 0 ? brace : bodyEnd;
    const typeTokens = tokens.slice(p, term);
    const fieldEnd = assign >= 0 ? findSemicolon(tokens, assign, bodyEnd) : term;

    for (const [a2, b2] of splitSpans(typeTokens, 0, typeTokens.length, ",")) {
      const chunk = typeTokens.slice(a2, b2);
      let nameTok = null;
      for (let k = chunk.length - 1; k >= 0; k--) {
        if (chunk[k].t === "id" && chunk[k].v !== "final") { nameTok = chunk[k]; break; }
      }
      if (!nameTok) continue;
      const idx = chunk.indexOf(nameTok);
      const typeText = renderType(chunk.slice(0, idx));
      if (!typeText) continue;
      cls.fields.push({
        name: nameTok.v,
        type: typeText,
        annotations: memberAnns,
        visibility,
        isStatic,
      });
    }

    const nextP = fieldEnd < bodyEnd && tokens[fieldEnd]?.v === ";" ? fieldEnd + 1 : fieldEnd;
    p = nextP > p ? nextP : p + 1;
  }

  return cls;
}

function skipBlockFrom(tokens, i, limit) {
  let k = i;
  while (k < limit && tokens[k].v !== "{") k++;
  if (k >= limit) return limit;
  return matchBrace(tokens, k, limit) + 1;
}

function findSemicolon(tokens, from, limit) {
  let depth = 0;
  for (let k = from; k < limit; k++) {
    const v = tokens[k].v;
    if (v === "{" || v === "(" || v === "[") depth++;
    else if (v === "}" || v === ")" || v === "]") depth--;
    else if (v === ";" && depth === 0) return k;
  }
  return limit;
}

function parseParams(tokens, start, end) {
  const params = [];
  for (const [a, b] of splitSpans(tokens, start, end, ",")) {
    const chunk = tokens.slice(a, b).filter((t) => t.v !== "final");
    const cleaned = [];
    for (let k = 0; k < chunk.length; k++) {
      if (chunk[k].v === "@") {
        const { next } = readAnnotationName(chunk, k);
        k = next - 1;
        if (chunk[k + 1]?.v === "(") k = matchParen(chunk, k + 1) ;
        continue;
      }
      cleaned.push(chunk[k]);
    }
    if (!cleaned.length) continue;
    let nameTok = null;
    for (let k = cleaned.length - 1; k >= 0; k--) {
      if (cleaned[k].t === "id") { nameTok = cleaned[k]; break; }
    }
    if (!nameTok) continue;
    const idx = cleaned.indexOf(nameTok);
    const typeText = renderType(cleaned.slice(0, idx));
    params.push({ name: nameTok.v, type: typeText || "Object" });
  }
  return params;
}

/* ------------------------------------------------------------- project load */

function walk(dir, acc = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, acc);
    else if (entry.name.endsWith(".java")) acc.push(full);
  }
  return acc;
}

const files = walk(SRC);
const classesByName = new Map();
const byStereotype = new Map();

for (const file of files) {
  const src = fs.readFileSync(file, "utf8");
  let parsed;
  try {
    parsed = parseJava(src, file);
  } catch (err) {
    console.error(`ERROR parseando ${file}: ${err.message}`);
    process.exitCode = 1;
    continue;
  }
  if (!parsed) continue;
  parsed.stereotype = stereotypeOf(parsed);
  if (classesByName.has(parsed.name)) {
    console.error(`ERROR: clase duplicada ${parsed.name} (${file})`);
    process.exitCode = 1;
  }
  classesByName.set(parsed.name, parsed);
  byStereotype.set(parsed.stereotype, (byStereotype.get(parsed.stereotype) ?? 0) + 1);
}

function stereotypeOf(c) {
  const has = (n) => c.annotations.some((a) => a.name === n);
  if (c.kind === "enum") return "Enumeration";
  if (has("Entity")) return "Entity";
  if (has("RestController") || has("Controller") || has("RestControllerAdvice") || has("ControllerAdvice")) return "Controller";
  if (has("Service")) return "Service";
  if (has("Repository")) return "Repository";
  if ([...c.implements, ...c.extends].some((t) => t.name === "JpaRepository")) return "Repository";
  if (has("Configuration")) return "Configuration";
  if (has("Scheduled")) return "Job";
  if (c.kind === "interface") return "Interface";
  return "Class";
}

/* --------------------------------------------------------------- relations */

function jpaTarget(field) {
  const rel = field.annotations.find((a) => JPA_RELATIONS.has(a.name));
  if (!rel || rel.name === "ElementCollection") return null;
  if (/mappedBy/.test(rel.args ?? "")) return null;
  const target = simpleTypeName(field.type);
  if (!target) return null;
  const joinColumn = field.annotations.find((a) => a.name === "JoinColumn");
  const optionalFalse =
    /optional\s*=\s*false/.test(rel.args ?? "") ||
    /nullable\s*=\s*false/.test(joinColumn?.args ?? "");
  return { rel: rel.name, target, optionalFalse, field: field.name };
}

function multiplicity(rel, optionalFalse) {
  switch (rel) {
    case "ManyToOne": return ["0..*", optionalFalse ? "1" : "0..1"];
    case "OneToMany": return ["1", "0..*"];
    case "OneToOne": return ["1", optionalFalse ? "1" : "0..1"];
    case "ManyToMany": return ["0..*", "0..*"];
    default: return ["0..*", "0..*"];
  }
}

function omittedField(view, cls, field) {
  const omit = view.polish?.omitFields?.[cls.name];
  if (omit?.includes(field.name)) return true;
  if (view.polish?.omitInverseCollections) {
    const rel = field.annotations.find((a) => JPA_RELATIONS.has(a.name));
    if (rel && /mappedBy/.test(rel.args ?? "")) return true;
  }
  return false;
}

function relationsFor(cls, inView, projectClasses, view) {
  const edges = [];
  const cut = new Set();
  const onlyAssociations = view.onlyAssociations === true;

  for (const field of cls.fields) {
    if (omittedField(view, cls, field)) continue;
    const hasJpaRelation = field.annotations.some((a) => JPA_RELATIONS.has(a.name));
    const jpa = jpaTarget(field);
    if (jpa) {
      if (inView.has(jpa.target)) {
        const [from, to] = multiplicity(jpa.rel, jpa.optionalFalse);
        edges.push({
          from: cls.name, to: jpa.target, kind: "association",
          label: jpa.field, fromMul: from, toMul: to,
        });
      } else if (projectClasses.has(jpa.target)) {
        cut.add(jpa.target);
      }
      continue;
    }

    // colecciones inversas (@OneToMany/@OneToOne con mappedBy) y @ElementCollection:
    // la relación ya se dibuja desde el lado dueño, no se agrega dependencia.
    if (hasJpaRelation) continue;
    if (onlyAssociations) continue;
    if (field.isStatic) continue;
    const base = simpleTypeName(field.type);
    if (!base) continue;
    if (base === cls.name) continue;
    if (inView.has(base)) {
      edges.push({ from: cls.name, to: base, kind: "dependency", label: "usa" });
    } else if (projectClasses.has(base)) {
      cut.add(base);
    }
  }

  for (const param of cls.ctorParams) {
    if (onlyAssociations) break;
    const base = simpleTypeName(param.type);
    if (!base || base === cls.name) continue;
    if (inView.has(base)) {
      edges.push({ from: cls.name, to: base, kind: "dependency", label: "inyecta" });
    } else if (projectClasses.has(base)) {
      cut.add(base);
    }
  }

  for (const t of cls.implements) {
    if (inView.has(t.name)) edges.push({ from: cls.name, to: t.name, kind: "realization", label: "implementa" });
    else if (EXTERNAL_INTERFACES.has(t.name)) edges.push({ from: cls.name, to: t.name, kind: "realization", label: "implementa", external: true });
  }
  for (const t of cls.extends) {
    if (inView.has(t.name)) edges.push({ from: cls.name, to: t.name, kind: "generalization", label: "extiende" });
    else if (EXTERNAL_INTERFACES.has(t.name)) edges.push({ from: cls.name, to: t.name, kind: "realization", label: "extiende", external: true });
    if (t.name === "JpaRepository" && t.args?.[0]) {
      edges.push({ from: cls.name, to: t.args[0], kind: "dependency", label: "gestiona" });
    }
  }

  return { edges, cut };
}

/* ------------------------------------------------------------------ emisor */

function mmType(type) {
  return String(type)
    .replace(/</g, "~")
    .replace(/>/g, "~")
    .replace(/,\s*/g, ",")
    .replace(/\s*\[\s*\]/g, "[]")
    .trim();
}

function visibilitySymbol(v) {
  if (v === "private") return "-";
  if (v === "protected") return "#";
  return "+";
}

function isAccessor(name) {
  return /^(get|set|is)[A-Z]/.test(name) ||
    ["hashCode", "equals", "toString", "canEqual"].includes(name);
}

function isApiMethod(m, stereotype) {
  if (m.isStatic || isAccessor(m.name)) return false;
  if (m.visibility === "private" || m.visibility === "protected") return false;
  if (stereotype === "Controller" || stereotype === "Interface" || stereotype === "Repository") return true;
  if (stereotype === "Service") return m.visibility === "public";
  return false;
}

function methodLine(m) {
  const params = m.params.length > 3
    ? m.params.slice(0, 2).map((p) => `${mmType(p.type)} ${p.name}`).join(", ") + ", ..."
    : m.params.map((p) => `${mmType(p.type)} ${p.name}`).join(", ");
  const ret = m.returnType && m.returnType !== "void" ? ` ${mmType(m.returnType)}` : "";
  return `+${m.name}(${params})${ret}`;
}

const MAX_METHODS = 8;

function emitClass(cls, view) {
  const lines = [];
  lines.push(`class ${cls.name} {`);
  lines.push(`    <<${cls.stereotype}>>`);

  if (cls.stereotype === "Enumeration") {
    for (const c of cls.enumConstants) lines.push(`    ${c}`);
    lines.push("}");
    return { lines, notes: [] };
  }

  for (const f of cls.fields) {
    if (f.isStatic) continue;
    if (omittedField(view, cls, f)) continue;
    lines.push(`    ${visibilitySymbol(f.visibility)}${mmType(f.type)} ${f.name}`);
  }

  const methods = cls.methods.filter((m) => isApiMethod(m, cls.stereotype));
  const shown = methods.slice(0, MAX_METHODS);
  for (const m of shown) lines.push(`    ${methodLine(m)}`);
  lines.push("}");

  const notes = [];
  if (methods.length > shown.length) {
    notes.push(`note for ${cls.name} "${shown.length} de ${methods.length} operaciones públicas"`);
  }
  if (cls.stereotype === "Controller") {
    const base = cls.annotations.find((a) => a.name === "RequestMapping");
    const routes = cls.methods.filter((m) =>
      m.annotations.some((a) => /Mapping$/.test(a.name) && a.name !== "RequestMapping")).length;
    const basePath = base?.args
      ? base.args.replace(/"/g, "").replace(/\s+/g, "")
      : "(sin base)";
    notes.push(`note for ${cls.name} "Rutas ${basePath} · ${routes} endpoints"`);
  }
  return { lines, notes };
}

function validateMermaid(text, file, expectLarge = false) {
  const declared = new Set();
  for (const m of text.matchAll(/^\s*class (\w+) \{/gm)) declared.add(m[1]);

  const relRe = /^\s*(\w+)\s+(?:"[^"]*"\s+)?(-->|\.\.>|\.\.\|>|--\|>)\s+(?:"[^"]*"\s+)?(\w+)/gm;
  const dangling = [];
  for (const m of text.matchAll(relRe)) {
    if (!declared.has(m[1]) || !declared.has(m[3])) dangling.push(`${m[1]} -> ${m[3]}`);
  }
  if (dangling.length) {
    console.error(`ERROR en ${file}: destinos no declarados -> ${dangling.join(", ")}`);
    process.exitCode = 1;
  }
  if (!expectLarge && declared.size > 35) {
    console.warn(`AVISO ${file}: ${declared.size} cajas (se recomienda partir la vista)`);
  }
  return declared.size;
}

function emitView(view, curated, projectClasses) {
  const missing = view.classes.filter((n) => !curated.has(n));
  if (missing.length) {
    console.error(`ERROR en ${view.file}: clases no encontradas -> ${missing.join(", ")}`);
    process.exitCode = 1;
    return null;
  }

  const inView = new Set(view.classes);
  const lines = [];
  lines.push("classDiagram");
  lines.push(`    %% ${view.title}`);
  lines.push("    %% Generado por generar.mjs desde backend-springboot/src/main/java");
  if (view.polish) {
    lines.push("    %% Vista depurada: sin colecciones inversas, FKs derivables, campos calculados ni actor de auditoría");
  }
  lines.push("");

  const notes = [];
  let omitted = 0;
  for (const name of view.classes) {
    const cls = curated.get(name);
    omitted += cls.fields.filter((f) => omittedField(view, cls, f)).length;
    const emitted = emitClass(cls, view);
    for (const l of emitted.lines) lines.push("    " + l);
    notes.push(...emitted.notes);
    lines.push("");
  }

  const externals = new Set();
  const edgeLines = [];
  const seen = new Set();
  const cut = new Set();

  for (const name of view.classes) {
    const cls = curated.get(name);
    const { edges, cut: c } = relationsFor(cls, inView, projectClasses, view);
    c.forEach((x) => cut.add(x));
    for (const e of edges) {
      if (!inView.has(e.to) && !e.external) continue;
      const key = `${e.from}|${e.to}|${e.kind}`;
      if (seen.has(key)) continue;
      seen.add(key);
      if (e.external) externals.add(e.to);
      if (e.kind === "association") {
        edgeLines.push(`    ${e.from} "${e.fromMul}" --> "${e.toMul}" ${e.to} : ${e.label}`);
      } else if (e.kind === "dependency") {
        edgeLines.push(`    ${e.from} ..> ${e.to} : ${e.label}`);
      } else if (e.kind === "realization") {
        edgeLines.push(`    ${e.from} ..|> ${e.to} : ${e.label}`);
      } else if (e.kind === "generalization") {
        edgeLines.push(`    ${e.from} --|> ${e.to} : ${e.label}`);
      }
    }
  }

  if (externals.size) {
    lines.push("    %% interfaces de framework referenciadas");
    for (const ext of externals) {
      lines.push(`    class ${ext} {`);
      lines.push("        <<interface>>");
      lines.push("    }");
    }
    lines.push("");
  }

  lines.push(...edgeLines);
  const noteLines = [];
  if (view.polish?.notes) {
    for (const [cls, text] of Object.entries(view.polish.notes)) {
      if (view.classes.includes(cls)) noteLines.push(`note for ${cls} "${text}"`);
    }
  }
  noteLines.push(...notes);
  if (noteLines.length) {
    lines.push("");
    lines.push(...noteLines.map((n) => "    " + n));
  }
  lines.push("");

  const text = lines.join("\n");
  const boxes = validateMermaid(text, view.file, view.expectLarge === true);
  fs.writeFileSync(path.join(OUT, view.file), text, "utf8");

  return {
    file: view.file,
    boxes,
    edges: edgeLines.length,
    omitted,
    cut: [...cut].sort(),
  };
}

/* -------------------------------------------------------------------- main */

const nucleo = JSON.parse(fs.readFileSync(path.join(OUT, "nucleo.json"), "utf8"));

// Expande las vistas que piden "todas las clases de un estereotipo" (ej. todas las @Entity).
const views = nucleo.views.map((view) => {
  if (!view.all) return view;
  let pool = [...classesByName.values()]
    .filter((c) => view.all.includes(c.stereotype))
    .map((c) => c.name);

  let dropped = [];
  if (view.dropIsolated) {
    const inPool = new Set(pool);
    const linked = new Set();
    for (const name of pool) {
      const cls = classesByName.get(name);
      for (const field of cls.fields) {
        if (omittedField(view, cls, field)) continue;
        const jpa = jpaTarget(field);
        if (jpa && inPool.has(jpa.target)) {
          linked.add(name);
          linked.add(jpa.target);
        }
      }
    }
    dropped = pool.filter((n) => !linked.has(n)).sort((a, b) => a.localeCompare(b));
    pool = pool.filter((n) => linked.has(n));
  }

  const ordered = [];
  for (const name of view.order ?? []) {
    if (pool.includes(name) && !ordered.includes(name)) ordered.push(name);
  }
  const rest = pool.filter((n) => !ordered.includes(n)).sort((a, b) => a.localeCompare(b));
  return { ...view, classes: [...ordered, ...rest], dropped };
});

const curated = new Map();
const missing = [];
for (const view of views) {
  for (const name of view.classes) {
    if (classesByName.has(name)) curated.set(name, classesByName.get(name));
    else if (!missing.includes(name)) missing.push(name);
  }
}
if (missing.length) {
  console.error(`ERROR: no existen en el código: ${missing.join(", ")}`);
  process.exit(1);
}

const projectClasses = new Set(classesByName.keys());
const report = [];

for (const view of views) {
  const r = emitView(view, curated, projectClasses);
  if (r) report.push(r);
}

const folderCounts = { modelo: 0, controladores: 0, servicios: 0, repositorios: 0, otros: 0 };
for (const f of files) {
  const rel = f.replace(/\\/g, "/");
  if (rel.includes("/modelo/")) folderCounts.modelo++;
  else if (rel.includes("/controladores/")) folderCounts.controladores++;
  else if (rel.includes("/servicios/")) folderCounts.servicios++;
  else if (rel.includes("/repositorios/")) folderCounts.repositorios++;
  else folderCounts.otros++;
}

const stats = {
  generatedAt: new Date().toISOString().slice(0, 10),
  totalJavaFiles: files.length,
  byFolder: folderCounts,
  byStereotype: Object.fromEntries([...byStereotype.entries()].sort()),
  curatedClasses: curated.size,
  views: report,
};

fs.writeFileSync(
  path.join(OUT, "modelo.json"),
  JSON.stringify(
    {
      stats,
      classes: [...curated.values()].map((c) => ({
        name: c.name,
        kind: c.kind,
        stereotype: c.stereotype,
        package: c.pkg,
        file: path.relative(ROOT, c.file).replace(/\\/g, "/"),
        extends: c.extends,
        implements: c.implements,
        enumConstants: c.enumConstants,
        fields: c.fields.map((f) => ({
          name: f.name,
          type: f.type,
          visibility: f.visibility,
          isStatic: f.isStatic,
          jpa: f.annotations
            .filter((a) => JPA_RELATIONS.has(a.name))
            .map((a) => ({ name: a.name, args: a.args })),
        })),
        ctorParams: c.ctorParams,
        methods: c.methods,
      })),
    },
    null,
    2
  ),
  "utf8"
);

console.log("Clases Java parseadas:", files.length);
console.log("Por carpeta:", JSON.stringify(folderCounts));
console.log("Por estereotipo:", JSON.stringify(stats.byStereotype));
console.log("Clases curadas en el núcleo:", curated.size);
console.log("");
for (const v of report) {
  const extra = v.omitted ? ` | ${v.omitted} campos omitidos` : "";
  console.log(`${v.file}  ->  ${v.boxes} cajas, ${v.edges} relaciones${extra}`);
  if (v.cut.length) console.log(`   referencias recortadas (fuera de la vista): ${v.cut.join(", ")}`);
}

for (const view of views) {
  if (view.dropped?.length) {
    console.log(`\n${view.file}: ${view.dropped.length} entidades sin relación excluidas`);
    console.log(`   ${view.dropped.join(", ")}`);
  }
}

if (process.argv.includes("--render")) {
  fs.mkdirSync(path.join(OUT, "render"), { recursive: true });
  const q = (s) => `"${s}"`;
  for (const view of views) {
    const input = path.join(OUT, view.file);
    const svg = path.join(OUT, "render", view.file.replace(/\.mmd$/, ".svg"));
    console.log(`Renderizando ${path.basename(svg)} ...`);
    const cmd = [
      "npx -y @mermaid-js/mermaid-cli",
      `-i ${q(input)}`,
      `-o ${q(svg)}`,
      `-c ${q(path.join(OUT, "mermaid-config.json"))}`,
      "-b white",
    ].join(" ");
    try {
      execSync(cmd, { stdio: "inherit", cwd: OUT });
    } catch (err) {
      console.error(`Fallo el render de ${view.file}: ${err.message}`);
      process.exitCode = 1;
    }
  }
}
