package com.library.seatmgmt.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility for persisting and retrieving Java objects via Object Serialization.
 * Demonstrates Generics (<T>), ObjectInputStream, ObjectOutputStream, and File I/O.
 */
public final class SerializationUtil {

    private SerializationUtil() {
    }

    /**
     * Serializes any Serializable object to a specified file path.
     */
    public static <T extends Serializable> boolean serialize(T object, String filePath) {
        if (object == null || filePath == null) {
            return false;
        }

        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
            LoggerUtil.info("Object serialized successfully to: " + filePath);
            return true;
        } catch (IOException e) {
            LoggerUtil.severe("Serialization failed for file: " + filePath, e);
            return false;
        }
    }

    /**
     * Deserializes an object of expected class type from a file path.
     * Returns null if the file does not exist or deserialization fails.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(String filePath, Class<T> clazz) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (clazz.isInstance(obj)) {
                LoggerUtil.info("Object deserialized successfully from: " + filePath);
                return (T) obj;
            } else {
                LoggerUtil.warning("Deserialized object type mismatch for: " + filePath);
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            LoggerUtil.severe("Deserialization failed for file: " + filePath, e);
            return null;
        }
    }
}
