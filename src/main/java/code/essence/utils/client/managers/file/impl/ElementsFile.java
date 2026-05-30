package code.essence.utils.client.managers.file.impl;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.utils.client.managers.api.draggable.DraggableRepository;
import code.essence.utils.client.managers.file.ClientFile;
import code.essence.utils.client.managers.file.exception.FileLoadException;
import code.essence.utils.client.managers.file.exception.FileSaveException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElementsFile extends ClientFile {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    DraggableRepository draggableRepository;

    public ElementsFile(DraggableRepository draggableRepository) {
        super("elements");
        this.draggableRepository = draggableRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        JsonObject elementsObject = new JsonObject();
        
        for (AbstractDraggable draggable : draggableRepository.draggable()) {
            JsonObject draggableObject = new JsonObject();
            int posX = draggable.getX();
            int posY = draggable.getY();
            float scaleFactor = draggable.getScaleFactor();
            draggableObject.addProperty("posX", posX);
            draggableObject.addProperty("posY", posY);
            draggableObject.addProperty("scaleFactor", scaleFactor);
            String name = draggable.getName().toLowerCase().replace(" ", "");
            elementsObject.add(name, draggableObject);
        }
        
        File file = new File(path, getName() + ".json");
        writeJsonToFile(elementsObject, file);
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        File file = new File(path, getName() + ".json");
        JsonObject elementsObject = readJsonFromFile(file);
        
        if (elementsObject != null) {
            updateElementsFromJsonObject(elementsObject);
        }
    }

    private void updateElementsFromJsonObject(JsonObject elementsObject) {
        for (AbstractDraggable draggable : draggableRepository.draggable()) {
            String name = draggable.getName().toLowerCase().replace(" ", "");
            JsonObject draggableObject = elementsObject.getAsJsonObject(name);
            if (draggableObject == null) continue;

            if (draggableObject.has("posX") && draggableObject.has("posY")) {
                int posX = draggableObject.get("posX").getAsInt();
                int posY = draggableObject.get("posY").getAsInt();
                draggable.setX(posX);
                draggable.setY(posY);
            }
            
            if (draggableObject.has("scaleFactor")) {
                float scaleFactor = draggableObject.get("scaleFactor").getAsFloat();
                draggable.setScaleFactor(scaleFactor);
            } else {
                
                draggable.setScaleFactor(1.0f);
            }
        }
    }

    private void writeJsonToFile(JsonObject jsonObject, File file) throws FileSaveException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(jsonObject, writer);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save elements to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        if (!file.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FileLoadException("Failed to load elements from file", e);
        } catch (JsonSyntaxException | JsonIOException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        }
    }
}










