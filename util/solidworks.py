import os
import win32com.client
import numpy as np
import matplotlib.pyplot as plt
from scipy.spatial import ConvexHull
import pythoncom
from win32com.client import VARIANT
from typing import List, Optional, Tuple, Any, Dict

# =============================================================================
# SolidWorks Part & Geometry Extraction
# =============================================================================

def find_sldprt_file(directory: str) -> Optional[str]:
    """
    Returns the first .sldprt file found in the specified directory.
    """
    for fname in os.listdir(directory):
        if fname.lower().endswith(".sldprt"):
            return os.path.join(directory, fname)
    return None

def load_part(swApp: Any, file_path: str) -> Any:
    """
    Opens the SolidWorks part file using the COM API and returns the ModelDoc2 object.
    """
    docType: int = 1  # swDocPART
    openOptions: int = 0
    err = VARIANT(pythoncom.VT_BYREF | pythoncom.VT_I4, 0)
    warn = VARIANT(pythoncom.VT_BYREF | pythoncom.VT_I4, 0)
    model = swApp.OpenDoc6(file_path, docType, openOptions, "", err, warn)
    print("OpenDoc6 error code:", err.value)
    print("OpenDoc6 warning code:", warn.value)
    if model is None:
        raise Exception(f"Failed to open part file. Error={err.value}, Warning={warn.value}")
    return model

def get_all_vertices(swModel: Any) -> List[Tuple[float, float, float]]:
    """
    Retrieves all vertex coordinates (x, y, z) from all bodies in the part.
    """
    vertices: List[Tuple[float, float, float]] = []
    bodies = swModel.GetBodies2(0, False)
    if bodies is None:
        return vertices
    for body in bodies:
        vtxs = body.GetVertices()
        if vtxs is None:
            continue
        for v in vtxs:
            # GetPoint is exposed as a property returning a tuple [x, y, z]
            pt = v.GetPoint
            if pt is not None and len(pt) >= 3:
                vertices.append((float(pt[0]), float(pt[1]), float(pt[2])))
    return vertices

# =============================================================================
# Geometry Processing Functions
# =============================================================================

def project_points(points: List[Tuple[float, float, float]], plane: str = "XY") -> np.ndarray:
    """
    Projects a list of 3D points onto the specified plane.
    plane: "XY" (drop Z), "XZ" (drop Y), or "YZ" (drop X).

    Returns:
        np.ndarray: Array of 2D points.
    """
    proj = []
    for x, y, z in points:
        if plane == "XY":
            proj.append([x, y])
        elif plane == "XZ":
            proj.append([x, z])
        elif plane == "YZ":
            proj.append([y, z])
    return np.array(proj)

def compute_convex_hull(points2d: np.ndarray) -> np.ndarray:
    """
    Computes the convex hull of 2D points using scipy's ConvexHull with 'QJ' option.

    Returns:
        np.ndarray: The points (in order) that form the convex hull.
    """
    if len(points2d) >= 3:
        hull = ConvexHull(points2d, qhull_options='QJ')
        return points2d[hull.vertices]
    # Fallback: sort the points
    sorted_pts = sorted(points2d.tolist(), key=lambda p: (p[0], p[1]))
    return np.array(sorted_pts)

def sample_polyline(polyline: np.ndarray, num_samples: int = 500) -> np.ndarray:
    """
    Densely samples a closed polyline using cumulative distance interpolation.

    Args:
        polyline (np.ndarray): An Nx2 array of 2D points (ordered along the polyline).
        num_samples (int): Number of sample points desired.

    Returns:
        np.ndarray: A (num_samples x 2) array of sampled points.
    """
    # Ensure polyline is closed.
    if not np.allclose(polyline[0], polyline[-1]):
        polyline = np.vstack([polyline, polyline[0]])
    diffs = np.diff(polyline, axis=0)
    seg_lengths = np.sqrt(np.sum(diffs**2, axis=1))
    cumdist = np.concatenate(([0], np.cumsum(seg_lengths)))
    total_length = cumdist[-1]

    # Equally spaced target distances.
    target_dists = np.linspace(0, total_length, num_samples)
    sampled = np.zeros((num_samples, 2))
    for dim in range(2):
        sampled[:, dim] = np.interp(target_dists, cumdist, polyline[:, dim])
    return sampled

def process_projection(vertices: List[Tuple[float, float, float]], plane: str) -> np.ndarray:
    """
    Given a list of 3D vertices, projects them onto the given plane,
    computes the convex hull, and densely samples the resulting silhouette.

    Args:
        vertices: List of 3D vertices.
        plane: Projection plane ("XY", "XZ", "YZ").

    Returns:
        np.ndarray: Sampled 2D points along the silhouette.
    """
    proj = project_points(vertices, plane)
    hull = compute_convex_hull(proj)
    sampled = sample_polyline(hull)
    return sampled

# =============================================================================
# Plotting & Output Helpers
# =============================================================================

def plot_projections(projections: Dict[str, np.ndarray]) -> None:
    """
    Plots projection curves for each view in a 1x3 subplot layout.
    1. Rotates each set of 2D points 90° counterclockwise.
    2. Shifts them so min x == 0.
    3. Plots the result.

    Args:
        projections: Dictionary mapping view names to 2D point arrays.
    """
    # Rotation matrix for 90° CCW: [[0, -1], [1, 0]]
    R = np.array([[0, -1],
                  [1,  0]])

    num_plots = len(projections)
    fig, axs = plt.subplots(1, num_plots, figsize=(6*num_plots, 6))
    if num_plots == 1:
        axs = [axs]

    for ax, (view, pts) in zip(axs, projections.items()):
        # 1) Rotate 90° CCW
        rot_pts = np.dot(pts, R.T)

        # 2) Shift so min x == 0
        min_x = np.min(rot_pts[:, 0])
        rot_pts[:, 0] -= min_x

        # 3) Plot
        ax.plot(rot_pts[:, 0], rot_pts[:, 1], '-o', markersize=0.1, linewidth=2, label=view)
        ax.set_title(view, fontsize=16)
        ax.set_xlabel('x', fontsize=14)
        ax.set_ylabel('y', fontsize=14)
        ax.grid(True)
        ax.legend()
        ax.axis('equal')

    plt.tight_layout()
    plt.show()


def print_projections(projections: Dict[str, np.ndarray]) -> None:
    """
    Prints projection points for each plane in the format "x: <value>, y: <value>".

    Args:
        projections: Dictionary with keys as plane names and values as 2D point arrays.
    """
    for plane, pts in projections.items():
        print(f"\n{plane} Projection Points:")
        for pt in pts:
            print("x: {:.6f}, y: {:.6f}".format(pt[0], pt[1]))

# =============================================================================
# Main Execution
# =============================================================================

def main() -> None:
    """
    Main routine:
      - Finds and loads a .sldprt file from the script's directory.
      - Extracts vertices from the part.
      - Processes the three projections (XY, XZ, YZ) and samples them.
      - Plots all three projection curves and prints their point data.
    """
    current_dir: str = os.path.dirname(os.path.abspath(__file__))
    file_path: Optional[str] = find_sldprt_file(current_dir)
    if not file_path:
        print("No .sldprt file found in", current_dir)
        return
    print("Using part file:", file_path)

    # Initialize SolidWorks COM object.
    swApp = win32com.client.Dispatch("SldWorks.Application")
    swApp.Visible = True
    swModel = load_part(swApp, file_path)
    vertices: List[Tuple[float, float, float]] = get_all_vertices(swModel)
    if not vertices:
        print("No vertices found in part.")
        return

    # Process projections for XY, XZ, and YZ.
    projection_planes = ["XY", "XZ", "YZ"]
    projections: Dict[str, np.ndarray] = {}
    for plane in projection_planes:
        projections[plane] = process_projection(vertices, plane)

    # Plot all projections.
    plot_projections(projections)
    # Print projection data.
    print_projections(projections)

if __name__ == '__main__':
    main()
