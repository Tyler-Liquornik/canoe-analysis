import re
import matplotlib.pyplot as plt
import numpy as np
from scipy.interpolate import CubicSpline

# Paste your log data as a multiline string below
# This is sample data for what it should look like format-wise from LoggerService::logPoints
log_data = """
02:10:44.691 [INFO] x: 0.0, y: 0.6160168685657584
02:10:44.691 [INFO] x: 0.005, y: 0.6102826148344151
02:10:44.691 [INFO] x: 0.01, y: 0.6046022417000867
02:10:44.691 [INFO] x: 0.015, y: 0.5989757491627734
02:10:44.691 [INFO] x: 0.02, y: 0.5934031372224748
02:10:44.691 [INFO] x: 0.025, y: 0.5878844058791912
02:10:44.691 [INFO] x: 0.03, y: 0.5824195551329225
02:10:44.691 [INFO] x: 0.035, y: 0.5770085849836688
02:10:44.691 [INFO] x: 0.04, y: 0.5716514954314301
02:10:44.691 [INFO] x: 0.045, y: 0.5663482864762062
02:10:44.691 [INFO] x: 0.05, y: 0.5610989581179973
"""

# ----- Settings -----

# Regular expression pattern to extract x and y values
pattern = r'x:\s*([-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),\s*y:\s*([-+]?\d*\.?\d+(?:[eE][-+]?\d+)?)'

# Flags for enabling modes:
ENABLE_INTERPOLATION = False  # Piecewise cubic interpolation with segmented fits
ENABLE_SINGLE_FIT = True  # Single overall best-fit parabola

# Validate that both modes are not enabled simultaneously
if ENABLE_INTERPOLATION and ENABLE_SINGLE_FIT:
    raise ValueError("Both interpolation and single fit modes are enabled. Please enable only one mode.")

# Scaling factor settings (used only for piecewise interpolation)
sharkbait_length = 6  # Original reference length
new_length = 5.71  # New desired length
scaling_factor = new_length / sharkbait_length

# ----- Data Extraction -----

x_values = []
y_values = []
lines = log_data.strip().split('\n')
for line in lines:
    match = re.search(pattern, line)
    if match:
        x_values.append(float(match.group(1)))
        y_values.append(float(match.group(2)))

if not x_values:
    print('No data extracted. Please check the format of your log data.')
else:
    # Convert to numpy arrays and sort by x
    x_values = np.array(x_values)
    y_values = np.array(y_values)
    sort_idx = np.argsort(x_values)
    x_values = x_values[sort_idx]
    y_values = y_values[sort_idx]

    plt.figure(figsize=(10, 6))
    plt.scatter(x_values, y_values, color='blue', marker='o', label='Data points')

    # ----- Piecewise Cubic Interpolation Mode -----
    if ENABLE_INTERPOLATION:
        # Define section endpoints and scale them
        section_endpoints = [0, 0.5, 2.88, 5.5, 6.0]
        section_endpoints = [ep * scaling_factor for ep in section_endpoints]

        # For each segment, perform cubic polynomial fit and plot
        for i in range(len(section_endpoints) - 1):
            start, end = section_endpoints[i], section_endpoints[i + 1]
            mask = (x_values >= start) & (x_values <= end)
            seg_x = x_values[mask]
            seg_y = y_values[mask]

            # Choose degree based on available points (prefer cubic if possible)
            if len(seg_x) < 4:
                deg = len(seg_x) - 1  # lower degree if not enough points
            else:
                deg = 3

            if deg < 0:
                continue  # Skip segments without enough data

            # Fit polynomial to segment data
            coeffs = np.polyfit(seg_x, seg_y, deg)
            poly = np.poly1d(coeffs)
            x_fine = np.linspace(start, end, 200)
            y_fine = poly(x_fine)

            # Helper: format each term in ax^n form
            def fmt(coef, power):
                if abs(coef) < 1e-6:
                    return ""
                sign = " + " if coef >= 0 else " - "
                coef_str = f"{abs(coef):.3f}"
                if power == 0:
                    term = coef_str
                elif power == 1:
                    term = f"{coef_str}x"
                else:
                    term = f"{coef_str}x^{power}"
                return sign + term

            if deg == 3:
                a, b, c, d = coeffs
                formula = f"{a:.3f}x^3" + fmt(b, 2) + fmt(c, 1) + fmt(d, 0)
            elif deg == 2:
                b, c, d = coeffs
                formula = f"{b:.3f}x^2" + fmt(c, 1) + fmt(d, 0)
            elif deg == 1:
                c, d = coeffs
                formula = f"{c:.3f}x" + fmt(d, 0)
            else:
                formula = f"{coeffs[0]:.3f}"

            plt.plot(x_fine, y_fine, linewidth=2,
                     label=f'Segment [{start:.2f}, {end:.2f}]: {formula}')

    # ----- Single Overall Best-Fit Parabola Mode -----
    if ENABLE_SINGLE_FIT:
        degree = 2
        coeffs = np.polyfit(x_values, y_values, degree)
        poly = np.poly1d(coeffs)
        x_fine = np.linspace(min(x_values), max(x_values), 500)
        y_fine = poly(x_fine)

        # Helper: format each term
        def fmt(coef, power):
            if abs(coef) < 1e-6:
                return ""
            sign = " + " if coef >= 0 else " - "
            coef_str = f"{abs(coef):.3f}"
            if power == 0:
                term = coef_str
            elif power == 1:
                term = f"{coef_str}x"
            else:
                term = f"{coef_str}x^{power}"
            return sign + term

        a, b, c = coeffs
        formula = f"{a:.3f}x^2" + fmt(b, 1) + fmt(c, 0)
        plt.plot(x_fine, y_fine, color='red', linewidth=2,
                 label=f'Overall Best-Fit Parabola: {formula}')

    # Finalize Plot
    plt.xlabel('x', fontsize=12)
    plt.ylabel('y', fontsize=12)
    plt.title('Data with Best-Fit Curve', fontsize=14)
    plt.grid(True)
    plt.legend()
    plt.show()
