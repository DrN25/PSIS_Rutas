package utils;

import java.util.ArrayList;
import java.util.List;

public class CSVUtils {
  public static String[] parseCSVLine(String line) {
        // Quitar las comillas del inicio y final de la línea si existen
        String cleanLine = line;
        if (cleanLine.startsWith("\"") && cleanLine.endsWith("\"") && cleanLine.length() > 1) {
            cleanLine = cleanLine.substring(1, cleanLine.length() - 1);
        }
        
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;
        
        for (int i = 0; i < cleanLine.length(); i++) {
            char currentChar = cleanLine.charAt(i);
            
            // Verificar si encontramos "" (dos comillas juntas)
            if (i < cleanLine.length() - 1 && currentChar == '"' && cleanLine.charAt(i + 1) == '"') {
                // Cambiar el estado de insideQuotes
                insideQuotes = !insideQuotes;
                // Agregar las dos comillas al campo
                currentField.append("\"\"");
                // Saltar ambas comillas
                i++;
            } else if (currentChar == ',' && !insideQuotes) {
                // Solo separamos por coma si NO estamos dentro de comillas dobles
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                // Cualquier otro caracter se agrega al campo actual
                currentField.append(currentChar);
            }
        }
        
        // Agregar el último campo
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }
}
