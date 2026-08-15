import os
import re

root_dir = r"c:\Users\lohit\Downloads\SpendWise\app\src\main\java"

for root, dirs, files in os.walk(root_dir):
    for f in files:
        if f.endswith(".kt"):
            fpath = os.path.join(root, f)
            with open(fpath, "r", encoding="utf-8", errors="ignore") as file:
                content = file.read()
            
            orig = content
            # Clean all corrupted chars
            content = content.replace("₹", "\\u20B9")
            content = content.replace("Ã¢â€šÂ¹", "\\u20B9")
            content = content.replace("Ã¢â‚¬Â¢", "\\u2022")
            content = content.replace("â‚¹", "\\u20B9")
            
            # Specific fixes
            if f == "DashboardScreen.kt":
                content = re.sub(r'label = if \(isSyncingSheet\) "[^"]*" else "[^"]*"', r'label = if (isSyncingSheet) "Syncing..." else "Sheet Sync"', content)
                content = re.sub(r'val currency = [^\n]+', r'val currency = "\\u20B9"', content)
                content = re.sub(r'else\s+"[^\"]*",\s+style = MaterialTheme\.typography\.headlineLarge', r'else "\\u2022\\u2022\\u2022\\u2022\\u2022\\u2022\\u2022\\u2022",\n                            style = MaterialTheme.typography.headlineLarge', content)
                content = re.sub(r'Toast\.makeText\(context,\s*"[^"]*Synced', r'Toast.makeText(context, "Synced', content)
            
            if f == "AppDatabase.kt":
                content = content.replace('currencySymbol = "₹"', 'currencySymbol = "\\u20B9"')
                content = content.replace("UPDATE monthly_budget SET currencySymbol = '₹'", "UPDATE monthly_budget SET currencySymbol = '\\u20B9'")

            if f == "MetricAndCharts.kt":
                content = re.sub(r'currencySymbol:\s*String\s*=\s*"[^"]*"', lambda m: 'currencySymbol: String = "\\u20B9"', content)

            if content != orig:
                with open(fpath, "w", encoding="utf-8") as file:
                    file.write(content)
                print(f"Cleaned {f}")

print("Done cleaning all kotlin files.")