namespace DotsAndBoxesClient;
using System;
using System.Text.Json;
using System.Text.Json.Serialization;

public class Int2DArrayConverter : JsonConverter<int[,]>
{
    public override int[,] Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject) throw new JsonException();

        var temp = new Dictionary<int, List<int>>();
        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject) break;

            var propertyName = reader.GetString();
            var key = int.Parse(propertyName);
            reader.Read();

            var value = JsonSerializer.Deserialize<List<int>>(ref reader, options);

            temp[key] = value;
        }

        // Convert dictionary back into int[,]
        int rowCount = temp.Count;
        int colCount = temp[0].Count; // Assuming all rows have the same length
        var result = new int[rowCount, colCount];

        for (int i = 0; i < rowCount; i++)
        {
            for (int j = 0; j < colCount; j++)
            {
                result[i, j] = temp[i][j];
            }
        }

        return result;
    }

    public override void Write(Utf8JsonWriter writer, int[,] value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();
        for (int i = 0; i < value.GetLength(0); i++)
        {
            writer.WritePropertyName(i.ToString());
            var row = new List<int>();

            for (int j = 0; j < value.GetLength(1); j++)
            {
                row.Add(value[i, j]);
            }

            JsonSerializer.Serialize(writer, row, options);
        }
        writer.WriteEndObject();
    }
}