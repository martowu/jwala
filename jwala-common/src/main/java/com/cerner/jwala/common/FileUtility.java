package com.cerner.jwala.common;

import com.cerner.jwala.common.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.Enumeration;;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * A utility class for file related operations
 *
 * Created by Jedd Anthony Cuison on 12/1/2016
 */
@Component
public class FileUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtility.class);

    /**
     * Unzips the file to the specified destination
     * @param destination the destination e.g. c:/scratch
     */
    public void unzip(final File zipFile, final File destination) {
        if (!destination.exists() && !destination.mkdir()) {
            throw new FileUtilityException("Failed to create zip file destination directory \"" + destination.getAbsolutePath() + "\"!");
        }
        JarFile jarFile = null;
        long startTime = System.currentTimeMillis();
        final String errMsg = MessageFormat.format("Failed to unpack {0}!", destination.getAbsolutePath());
        try {
            LOGGER.debug("Start unzip {}", zipFile.getAbsoluteFile());
            jarFile = new JarFile(zipFile);
            final Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = (JarEntry) entries.nextElement();
                final File f = new File(destination + File.separator + jarEntry.getName());
                if (jarEntry.isDirectory()) {
                    if (!f.mkdir()) {
                        throw new ApplicationException("Failed to create directory " + jarEntry.getName());
                    }
                    continue;
                }
                final InputStream in = jarFile.getInputStream(jarEntry);
                final FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bufout = new BufferedOutputStream(fos);

                while (in.available() > 0) {
                    bufout.write(in.read());
                }
                bufout.close();
                in.close();
            }
        } catch (final IOException e) {
            throw new FileUtilityException("Failed to unpack " + zipFile.getAbsolutePath() + "!", e);
        } finally {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException e) {
                throw new FileUtilityException(errMsg, e);
            }
        }
        LOGGER.debug("End unzip {} in {} ms", zipFile.getAbsoluteFile(), (System.currentTimeMillis() - startTime));
        }
    }
    public void createJarArchive(File archiveFile, File[] filesToBeJared, String parent) {

        final int BUFFER_SIZE = 10240;
        try (
                FileOutputStream stream = new FileOutputStream(archiveFile);
                JarOutputStream out = new JarOutputStream(stream, new Manifest())) {

            byte buffer[] = new byte[BUFFER_SIZE];

            // Open archive file
            for (File aFileTobeJared : filesToBeJared) {
                if (aFileTobeJared == null || !aFileTobeJared.exists() || aFileTobeJared.isDirectory()) {
                    continue; // Just in case...
                }

                LOGGER.debug("Adding " + aFileTobeJared.getPath());

                File parentDir = new File(parent);

                String relPath = aFileTobeJared.getCanonicalPath()
                        .substring(parentDir.getParentFile().getCanonicalPath().length() + 1,
                                aFileTobeJared.getCanonicalPath().length());

                relPath = relPath.replace("\\", "/");

                // Add archive entry
                JarEntry jarAdd = new JarEntry(relPath);
                jarAdd.setTime(aFileTobeJared.lastModified());
                out.putNextEntry(jarAdd);


                // Write file to archive
                try (FileInputStream in = new FileInputStream(aFileTobeJared)) {
                    while (true) {
                        int nRead = in.read(buffer, 0, buffer.length);
                        if (nRead <= 0)
                            break;
                        out.write(buffer, 0, nRead);
                    }
                }
            }

            LOGGER.debug("Adding files to jar completed");
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    public static void main(String args[]){
        try{
        FileUtility fileUtility = new FileUtility();
        long startTime = System.currentTimeMillis();
        System.out.println("Start unzip..");
        fileUtility.unzip(new File("C:\\dev\\apache-tomcat-7.0.55\\data\\binaries\\apache-tomcat-7.0.55-linux.zip"),new File("C:\\dev\\deleteme"));
        System.out.println("Time taken: "+(System.currentTimeMillis()-startTime)/1000);
        }catch(Throwable th){
            th.printStackTrace();
        }
    }
}