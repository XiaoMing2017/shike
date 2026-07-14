import mysql.connector

try:
    conn = mysql.connector.connect(
        host="117.72.61.18",
        user="root",
        password="Nizouba12138.",
        database="db_shike"
    )
    cursor = conn.cursor()
    cursor.execute("SELECT id, food_items, total_calories FROM tb_diet_record ORDER BY id DESC LIMIT 5")
    rows = cursor.fetchall()
    print("Latest 5 records:")
    for row in rows:
        print(f"ID: {row[0]}")
        # print the raw bytes or representation
        print(f"Food Items (repr): {repr(row[1])}")
        print(f"Total Calories: {row[2]}")
        print("-" * 50)
    cursor.close()
    conn.close()
except Exception as e:
    print(f"Error connecting/querying local MySQL: {e}")
