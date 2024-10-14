import re
import matplotlib.pyplot as plt

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

# Initialize lists to hold x and y values
x_values = []
y_values = []

# Regular expression pattern to extract x and y values
pattern = r'x:\s*([-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),\s*y:\s*([-+]?\d*\.?\d+(?:[eE][-+]?\d+)?)'

# Split the log data into lines
lines = log_data.strip().split('\n')

# Iterate over each line and extract x and y values
for line in lines:
    match = re.search(pattern, line)
    if match:
        x_val = float(match.group(1))
        y_val = float(match.group(2))
        x_values.append(x_val)
        y_values.append(y_val)

# Check if data was extracted
if not x_values:
    print('No data extracted. Please check the format of your log data.')
else:
    # Create scatter plot
    plt.figure(figsize=(10, 6))
    plt.scatter(x_values, y_values, color='blue', marker='o')

    # Set labels and title
    plt.xlabel('x', fontsize=12)
    plt.ylabel('y', fontsize=12)
    plt.title('Scatter Plot of x vs. y from Log Data', fontsize=14)

    # Show grid
    plt.grid(True)

    # Display the plot
    plt.show()
