#!/usr/bin/env python3
"""Emit a deterministic structural ABI fingerprint for compat .class files.

The Phase 4 structural gate intentionally compares class identity, JVM major version,
class/field/method access flags, field descriptors and ConstantValue attributes, and
method descriptors. Method bytecode is deliberately not included here; behavioral
control-flow equivalence is audited separately during Phase 4.
"""

import hashlib
import io
import os
import struct
import sys
import zipfile


def u1(f): return struct.unpack(">B", f.read(1))[0]
def u2(f): return struct.unpack(">H", f.read(2))[0]
def u4(f): return struct.unpack(">I", f.read(4))[0]
def u8(f): return struct.unpack(">Q", f.read(8))[0]


class ConstantPool:
    def __init__(self, f):
        count = u2(f)
        self.items = [None] * count
        i = 1
        while i < count:
            tag = u1(f)
            if tag == 1:
                size = u2(f)
                self.items[i] = ("Utf8", f.read(size).decode("utf-8", "replace"))
            elif tag in (3, 4):
                self.items[i] = ("Integer" if tag == 3 else "Float", u4(f))
            elif tag in (5, 6):
                self.items[i] = ("Long" if tag == 5 else "Double", u8(f))
                i += 1
            elif tag in (7, 8, 16, 19, 20):
                self.items[i] = (tag, u2(f))
            elif tag in (9, 10, 11, 12, 17, 18):
                self.items[i] = (tag, u2(f), u2(f))
            elif tag == 15:
                self.items[i] = (tag, u1(f), u2(f))
            else:
                raise ValueError(f"unsupported constant-pool tag {tag}")
            i += 1

    def utf8(self, index):
        item = self.items[index]
        return item[1] if item and item[0] == "Utf8" else None

    def class_name(self, index):
        item = self.items[index]
        return self.utf8(item[1]) if item and item[0] == 7 else None

    def constant(self, index):
        item = self.items[index]
        if not item:
            return None
        if item[0] == "Integer":
            return str(struct.unpack(">i", struct.pack(">I", item[1]))[0])
        if item[0] == "Float":
            return repr(struct.unpack(">f", struct.pack(">I", item[1]))[0])
        if item[0] == "Long":
            return str(struct.unpack(">q", struct.pack(">Q", item[1]))[0])
        if item[0] == "Double":
            return repr(struct.unpack(">d", struct.pack(">Q", item[1]))[0])
        if item[0] == 8:
            return repr(self.utf8(item[1]))
        return None


def read_attributes(f, cp):
    result = []
    for _ in range(u2(f)):
        name = cp.utf8(u2(f))
        size = u4(f)
        result.append((name, f.read(size)))
    return result


def parse_class(data):
    f = io.BytesIO(data)
    if u4(f) != 0xCAFEBABE:
        raise ValueError("not a classfile")
    u2(f)  # minor
    major = u2(f)
    cp = ConstantPool(f)
    class_flags = u2(f)
    this_class = cp.class_name(u2(f))
    super_index = u2(f)
    super_class = cp.class_name(super_index) if super_index else "-"
    interfaces = [cp.class_name(u2(f)) for _ in range(u2(f))]

    fields = []
    for _ in range(u2(f)):
        flags = u2(f)
        name = cp.utf8(u2(f))
        descriptor = cp.utf8(u2(f))
        constant = None
        for attr_name, attr_data in read_attributes(f, cp):
            if attr_name == "ConstantValue":
                constant = cp.constant(struct.unpack(">H", attr_data)[0])
        fields.append((name, descriptor, flags, constant))

    methods = []
    for _ in range(u2(f)):
        flags = u2(f)
        name = cp.utf8(u2(f))
        descriptor = cp.utf8(u2(f))
        read_attributes(f, cp)
        methods.append((name, descriptor, flags))

    read_attributes(f, cp)
    return major, class_flags, this_class, super_class, interfaces, fields, methods


def class_inputs(path):
    if os.path.isdir(path):
        for root, _, files in os.walk(path):
            for filename in files:
                if not filename.endswith(".class"):
                    continue
                full = os.path.join(root, filename)
                rel = os.path.relpath(full, path).replace(os.sep, "/")
                if rel.startswith("dev/cchqphysics/compat/"):
                    with open(full, "rb") as handle:
                        yield rel, handle.read()
    else:
        with zipfile.ZipFile(path) as archive:
            for name in sorted(archive.namelist()):
                if name.startswith("dev/cchqphysics/compat/") and name.endswith(".class"):
                    yield name, archive.read(name)


def canonical(data):
    major, flags, name, super_name, interfaces, fields, methods = parse_class(data)
    lines = [
        f"CLASS {name} major={major} flags=0x{flags:04x} "
        f"super={super_name} interfaces={','.join(interfaces) if interfaces else '-'}"
    ]
    for field_name, descriptor, field_flags, constant in sorted(fields, key=lambda x: (x[0], x[1])):
        line = f"FIELD {field_name} {descriptor} flags=0x{field_flags:04x}"
        if constant is not None:
            line += f" const={constant}"
        lines.append(line)
    for method_name, descriptor, method_flags in sorted(methods, key=lambda x: (x[0], x[1])):
        lines.append(f"METHOD {method_name} {descriptor} flags=0x{method_flags:04x}")
    return name, fields, methods, "\n".join(lines) + "\n"


def main():
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {sys.argv[0]} <jar-or-classes-dir>")
    rows = []
    for _, data in class_inputs(sys.argv[1]):
        name, fields, methods, text = canonical(data)
        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        rows.append((name, digest, len(fields), len(methods)))
    for name, digest, field_count, method_count in sorted(rows):
        print(f"{digest}  {name}  fields={field_count} methods={method_count}")


if __name__ == "__main__":
    main()
