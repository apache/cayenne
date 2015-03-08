package de.jexp.jequel.util;

import java.io.*;
import java.net.URL;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 02:27:09
 */
public class FileUtils {
    public static File assertWriteJavaFile(final String basePath, final String javaPackage, final String javaClass) {
        final File path = getFileForPackage(basePath, javaPackage);
        if (cantWriteToPath(path)) {
            path.mkdirs();
        }
        if (cantWriteToPath(path))
            throw new IllegalArgumentException("Can't write to directory " + path);
        final File file = new File(path, javaClass + ".java");
        if (file.exists() && !file.canWrite() || file.isDirectory())
            throw new IllegalArgumentException("Can't write to java source file " + file);
        return file;
    }

    public static File getFileForPackage(final String basePath, final String javaPackage) {
        final String packagePath = javaPackage.replace('.', File.separatorChar);
        return new File(basePath, packagePath);
    }

    public static boolean cantWriteToPath(final File path) {
        return !path.exists() || !path.canWrite() || !path.isDirectory();
    }

    public static void writeFile(final File file, final String javaSourceFile) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(javaSourceFile);
        } catch (IOException ioe) {
            throw new RuntimeException("Error writing to file " + file, ioe);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static String processFile(final File file, final LineProcessor lineProcessor) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            final StringBuilder result = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                final String processedLine = lineProcessor.processLine(line);
                if (processedLine != null)
                    result.append(processedLine).append('\n');
            }
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error reading file " + file, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static File getJavaFile(final Class<?> javaClass) {
        final String fileName = getJavaFileName(javaClass);
        final URL fileUrl = ClassLoader.getSystemResource(fileName);
        if (fileUrl != null && fileUrl.getProtocol().equals("file")) return new File(fileUrl.getPath());
        throw new RuntimeException("File not found " + fileName);
    }

    public static String getJavaFileName(final Class<?> javaClass) {
        return javaClass.getName().replace('.', File.separatorChar) + ".java";
    }

    public static String readFileToString(final String path) {
        return readFileToString(new File(path));
    }

    public static String readFileToString(final File path) {
        final StringBuilder result = new StringBuilder();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(path);
            int size;
            final char[] buffer = new char[5000];
            while ((size = fileReader.read(buffer)) > 0) {
                result.append(buffer, 0, size);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileReader != null) try {
                fileReader.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return result.toString();
    }

    public static File getJavaFile(final String srcPrefix, final Class<?> javaClass) {
        return new File(srcPrefix, getJavaFileName(javaClass));
    }

    public interface LineProcessor {
        String processLine(final String line);
    }
}
