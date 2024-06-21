package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a gitlet stagingArea object. */
public class StagingArea implements Serializable, Dumpable {
    @Serial
    private static final long serialVersionUID = -8161863113995390299L;
    /** The map from the name of the to-be-added file to the blob ID. */
    private Map<String, String> stagedForAddition;
    /** The to-be-removed file list. */
    private List<String> stagedForRemoval;

    public StagingArea() {
        if (Repository.GITLET_DIR.exists()) {
            File stagingAreaFile = Utils.join(Repository.GITLET_DIR, "stagingArea");
            if (stagingAreaFile.exists()) {
                StagingArea stagingArea = Utils.readObject(stagingAreaFile, StagingArea.class);
                this.stagedForAddition = stagingArea.stagedForAddition;
                this.stagedForRemoval = stagingArea.stagedForRemoval;
            } else {
                this.stagedForAddition = new HashMap<>();
                this.stagedForRemoval = new ArrayList<>();
                saveStagingArea();
            }
        }
    }

    /** Returns the map stagedForAddition. */
    public Map<String, String> getStagedForAddition() {
        return stagedForAddition;
    }

    /** Returns the list stagedForRemoval. */
    public List<String> getStagedForRemoval() {
        return stagedForRemoval;
    }

    /** Saves the staging area to a file for future use.
     * @source modified from lab6.Dog.saveDog */
    public void saveStagingArea() {
        File saveFile = Utils.join(Repository.GITLET_DIR, "stagingArea");
        Utils.writeObject(saveFile, this);
    }

    /** Adds a file to stagedForAddition.
     * (Unlike to the real Git, our gitlet allows only one file to be added at one time.)
     *
     * Staging an already-staged file overwrites the previous entry
     * in the staging area with the new contents.
     * If the current working version of the file is
     * identical to the version in the current commit,
     * do not stage it to be added, and remove it from the staging area
     * if it is already there (as can happen when a file is changed, added,
     * and then changed back to its original version).
     * The file will no longer be staged for removal
     * if it was at the time of the command.
     *
     * @param fileName the name of the to-be-added file
     * @param currentCommitID the SHA1 ID of the current commit (the head)
     */
    public void addFile(String fileName, String currentCommitID) {
        // If the file is now in the stagedForRemoval area,
        // then delete it from stagedForRemoval in order not to remove it.
        if (stagedForRemoval.contains(fileName)) {
            stagedForRemoval.remove(fileName);
        }

        Commit currentCommit = Commit.fromFile(currentCommitID);
        Map<String, String> mapFileNameToBlobIDInCurrentCommit = currentCommit.mapFileNameToBlob;

        File fileToBeAdded = new File(fileName);
        String newFileContent = Utils.readContentsAsString(fileToBeAdded);

        if (mapFileNameToBlobIDInCurrentCommit.containsKey(fileName)) {
            String oldBlobID = mapFileNameToBlobIDInCurrentCommit.get(fileName);
            String fileContentInOldBlob = Blob.contentFromFile(oldBlobID);

            if (newFileContent.equals(fileContentInOldBlob)) {
                return;
            } else { // just add the modified file to the staging area
                Blob newBlob = new Blob(newFileContent);
                stagedForAddition.put(fileName, newBlob.getBlobID());
                newBlob.saveBlob();
            }
        } else { // the file is not tracked by the current commit
            Blob newBlob = new Blob(newFileContent);
            stagedForAddition.put(fileName, newBlob.getBlobID());
            newBlob.saveBlob();
        }
    }

    /** Adds a file to stagedForRemoval in order to remove it from the repository.
     *
     * Removes the file from the staged state if it is currently in stagedForAddition.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from the working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     *
     * @param fileName the name of the to-be-removed file
     * @param currentCommitID the SHA1 ID of the current commit (the head)
     */
    public void removeFile(String fileName, String currentCommitID) {
        if (stagedForAddition.containsKey(fileName)) { // remove the file in stagedForAddition
            stagedForAddition.remove(fileName);
        }

        Commit currentCommit = Commit.fromFile(currentCommitID);
        Map<String, String> mapFileNameToBlobIDInCurrentCommit = currentCommit.mapFileNameToBlob;
        if (mapFileNameToBlobIDInCurrentCommit.containsKey(fileName)) {
            stagedForRemoval.add(fileName);
            boolean successfullyRemoved = Utils.restrictedDelete(fileName);
        }
    }

    /** Clears all the files from stagedForAddition and stagedForRemoval.
     * After the file is committed, the staging area need to be empty.
     *
     * This method does not save the cleared stagedForAddition and stagedFroRemoval to file.
     */
    public void clear() {
        stagedForAddition.clear();
        stagedForRemoval.clear();
    }

    @Override
    public void dump() {
        System.out.println("stagedForAddition: ");
        for (String fileName : stagedForAddition.keySet()) {
            System.out.printf("file name: %s to blob ID: %s%n",
                    fileName, stagedForAddition.get(fileName));
        }
        System.out.println("stagedForRemoval: ");
        for (String fileName : stagedForRemoval) {
            System.out.print(fileName + " ");
        }
        System.out.println();
    }
}
