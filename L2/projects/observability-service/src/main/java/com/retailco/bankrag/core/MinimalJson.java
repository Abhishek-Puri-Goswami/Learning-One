package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// CONCEPT: Hand-rolled recursive-descent parser -- a small, from-scratch
// JSON reader/writer.
// PURPOSE: OpenAiEmbeddingModel needs to send and parse JSON to talk to
// OpenAI's REST API, but this module deliberately has zero external
// dependencies (no Jackson/Gson). This class exists purely as a minimal,
// dependency-free substitute -- it understands only the JSON shapes
// OpenAI's Embeddings/Chat Completions endpoints actually use, not the
// full JSON spec.
// HOW IT WORKS: `parse()` wraps the string in a `Parser` and calls
// `parseValue()`, which looks at the next character to decide what kind of
// JSON value follows ('{' object, '[' array, '"' string, 't'/'f' boolean,
// 'n' null, else a number) and recurses accordingly -- this is the
// "recursive descent" parsing technique, one parsing method per grammar
// rule. `asObject`/`asArray`/`asDouble`/`asInt` are small unchecked casts
// that make call sites read cleanly once you know the expected shape.
// WHY: in a real production project you'd use Jackson or Gson instead of
// hand-rolling this -- it's a deliberate, disclosed trade-off for staying
// dependency-free, not a recommended general-purpose JSON library.
final class MinimalJson {

    private MinimalJson() {
    }

    // ---- Writing -----------------------------------------------------

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ---- Reading -------------------------------------------------------

    static Object parse(String json) {
        Parser p = new Parser(json);
        Object value = p.parseValue();
        p.skipWhitespace();
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asArray(Object o) {
        return (List<Object>) o;
    }

    static double asDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    static int asInt(Object o) {
        return ((Number) o).intValue();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
            this.i = 0;
        }

        void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object parseValue() {
            skipWhitespace();
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skipWhitespace();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = s.charAt(i++);
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or } at " + i);
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWhitespace();
            if (peek() == ']') {
                i++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char c = s.charAt(i++);
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or ] at " + i);
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            String hex = s.substring(i, i + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            } else if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at " + i);
        }

        Object parseNull() {
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid literal at " + i);
        }

        Double parseNumber() {
            int start = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(start, i));
        }

        char peek() {
            return s.charAt(i);
        }

        void expect(char c) {
            if (s.charAt(i) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at " + i);
            }
            i++;
        }
    }
}
