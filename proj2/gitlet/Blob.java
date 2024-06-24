package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/** Represents a gitlet blob object. */
public class Blob implements Serializable, Dumpable {
    @Serial
    private static final long serialVersionUID = -5528835260037188238L;
    /** The SHA1 ID of this blob. */
    private String blobID;
    /** The file content this blob pointing at. */
    private String fileContent;

    public Blob(String content) {
        this.fileContent = content;
        // the blob ID is created only depending on
        // the file content contained in the blob file.
        this.blobID = Utils.sha1(Utils.serialize(content));
    }

    /** Returns the SHA1 ID of this blob. */
    public String getBlobID() {
        return blobID;
    }

    /** Returns the file content this blob pointing at. */
    public String getFileContent() {
        return fileContent;
    }

    /**
     * Reads in and deserializes a blob from a file with the name of blob SHA1 ID in BLOB_DIR.
     * Returns null if no file with the given blob ID exists.
     *
     * @param requiredBlobID the name of the branch to load
     * @return the blob
     * @source modified from lab6.Dog.fromFile
     */
    public static Blob fromFile(String requiredBlobID) {
        File blobFile = Utils.join(Repository.BLOB_DIR, requiredBlobID);
        if (!blobFile.exists()) {
            return null;
        }
        return Utils.readObject(blobFile, Blob.class);
    }

    /** Returns the file content contained in the blob file with the blob ID as its name.
     * Returns null if no file with the given blob ID exists.
     *
     * @param requiredBlobID the name of the branch to load
     * @return the file content in the blob file
     */
    public static String contentFromFile(String requiredBlobID) {
        Blob requiredBlob = fromFile(requiredBlobID);
        if (requiredBlob == null) {
            return null;
        }
        return requiredBlob.fileContent;
    }

    /** Saves a blob to a file for future use.
     * @source modified from lab6.Dog.saveDog
     */
    public void saveBlob() {
        if (!Repository.BLOB_DIR.exists()) {
            Repository.BLOB_DIR.mkdir();
        }
        File saveFile = Utils.join(Repository.BLOB_DIR, blobID);
        Utils.writeObject(saveFile, this);
    }

    @Override
    public void dump() {
        System.out.printf("blob ID: %s%nfile content: %s%n", blobID, fileContent);
    }
}
