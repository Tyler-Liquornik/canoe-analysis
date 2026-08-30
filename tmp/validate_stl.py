from pathlib import Path
import struct
import numpy as np


FILES = [
    Path(r"C:\Users\perry\Downloads\0.21.07.02F (Final Profile).STL"),
    Path(r"C:\Users\perry\Downloads\0.21.07.02F (Final Profile + Stuctural Elements).STL"),
]


def load_binary_stl(path: Path):
    raw = path.read_bytes()
    count = struct.unpack_from("<I", raw, 80)[0]
    expected = 84 + count * 50
    if expected != len(raw):
        raise ValueError(f"Not binary STL or malformed: {path} ({len(raw)=}, {expected=})")
    facets = np.frombuffer(
        raw,
        dtype=np.dtype([
            ("normal", "<f4", (3,)),
            ("vectors", "<f4", (3, 3)),
            ("attr", "<u2"),
        ]),
        count=count,
        offset=84,
    )
    return facets["vectors"].astype(np.float64)


for path in FILES:
    triangles = load_binary_stl(path)
    vertices = triangles.reshape(-1, 3)
    minimum = vertices.min(axis=0)
    maximum = vertices.max(axis=0)
    span = maximum - minimum
    cross = np.cross(triangles[:, 1] - triangles[:, 0], triangles[:, 2] - triangles[:, 0])
    area = 0.5 * np.linalg.norm(cross, axis=1).sum()
    signed_volume = np.einsum(
        "ij,ij->i", triangles[:, 0], np.cross(triangles[:, 1], triangles[:, 2])
    ).sum() / 6.0
    print(path.name)
    print(f"triangles={len(triangles)}")
    print(f"min_mm={minimum.tolist()}")
    print(f"max_mm={maximum.tolist()}")
    print(f"span_mm={span.tolist()}")
    print(f"surface_area_mm2={area:.6f}")
    print(f"signed_volume_mm3={signed_volume:.6f}")
    print(f"absolute_volume_m3={abs(signed_volume) / 1e9:.9f}")
    print()
