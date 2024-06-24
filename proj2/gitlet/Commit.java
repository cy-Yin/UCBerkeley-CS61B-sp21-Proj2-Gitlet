package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date; // You'll likely use this in this class
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  Combinations of log messages (git commit -m [message]),
 *  other metadata (commit date, author, etc.),
 *  references to the two parent commits,
 *  and references to the blobs about the file versions.
 *  The commit instance also maintains a mapping from branch heads to references to commits,
 *  so that certain important commits have symbolic names (commit SHA1 IDs).
 *
 *  @author C. Yin
 */
public class Commit implements Serializable, Dumpable {
    @Serial
    private static final long serialVersionUID = 1323192079276861884L;
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The timestamp of this Commit. */
    private Date timeStamp;
    /** The first parent of this Commit. */
    private String parent1;
    /** The second parent of this Commit.
     * Unlike to the real Git, our gitlet just maintains two parent for each commit.
     */
    private String parent2;
    /** The filename-to-contents map of this Commit.
     *  Mapping the file names to the blobs IDs. (String to String)
     */
    Map<String, String> mapFileNameToBlob = new HashMap<>();
    /** The SHA1 ID of this Commit. */
    private String commitID;

    /** Creates the sentinel commit instance when calling {@code java gitlet.Main init} */
    public Commit() {
        this.message = "initial commit";
        this.parent1 = null;
        this.parent2 = null;
        this.timeStamp = new Date(0); // Thu Jan 00 00:00:00 UTC 1970
        this.commitID = Utils.sha1(Utils.serialize(this));
    }

    /** the commit instance */
    public Commit(String message, String parent1, String parent2,
                                    Map<String, String> mapFileNameToBlob) {
        this.message = message;
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.timeStamp = new Date();
        this.mapFileNameToBlob = mapFileNameToBlob;
        this.commitID = Utils.sha1(Utils.serialize(this));
    }

    /** Returns the message of this commit. */
    public String getMessage() {
        return message;
    }

    /** Returns the first parent of this commit. */
    public String getFirstParent() {
        return parent1;
    }

    /** Returns the second parent of this commit. */
    public String getSecondParent() {
        return parent2;
    }

    /** Returns the SHA1 ID of this commit. */
    public String getCommitID() {
        return commitID;
    }

    /**
     * Reads in and deserializes a commit from a file with name commitID in COMMIT_DIR.
     * Returns null if no file with the given commit ID exists.
     *
     * @param requiredCommitID the ID of the commit to load (can be abbreviated)
     * @return the commit
     * @source modified from lab6.Dog.fromFile
     */
    public static Commit fromFile(String requiredCommitID) {
        final int standardCommitIDLength = 40;
        int requiredCommitIDLength = requiredCommitID.length();
        // if the commit ID is abbreviated
        if (requiredCommitIDLength < standardCommitIDLength) {
            return shortFromFile(requiredCommitID);
        }

        File commitFile = Utils.join(Repository.COMMIT_DIR, requiredCommitID);
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     * Reads in and deserializes a commit from a file with the name
     * matching the short less-than-40-long SHA1 ID in COMMIT_DIR.
     * Returns null if no file with the given commit ID exists.
     *
     * @param shortCommitID the ID of the commit to load
     * @return the commit
     * @source modified from lab6.Dog.fromFile
     */
    public static Commit shortFromFile(String shortCommitID) {
        int shortLength = shortCommitID.length();
        String matchedCommitID = "";
        for (String commitID : Utils.plainFilenamesIn(Repository.COMMIT_DIR)) {
            if (commitID.substring(0, shortLength).equals(shortCommitID)) {
                matchedCommitID = commitID;
            }
        }
        File commitFile = Utils.join(Repository.COMMIT_DIR, matchedCommitID);
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Saves a commit to a file for future use.
     * @source modified from lab6.Dog.saveDog
     */
    public void saveCommit() {
        if (!Repository.COMMIT_DIR.exists()) {
            Repository.COMMIT_DIR.mkdir();
        }
        File saveFile = Utils.join(Repository.COMMIT_DIR, commitID);
        Utils.writeObject(saveFile, this);
    }

    /** Returns the content of the file in this commit whose name matches the given file name.
     * Returns null if the {@code mapFileNameToBlob} of this commit does not contain this file.
     */
    public String getFileContentInCommit(String fileName) {
        if (!mapFileNameToBlob.containsKey(fileName)) {
            return null;
        }
        String fileBlobID = mapFileNameToBlob.get(fileName);
        String fileContent = Blob.contentFromFile(fileBlobID);
        return fileContent;
    }

    /** Commit instances will be presented when calling java gitlet.Main log as follows.
     * -----------------------------------------------------------------
     * ===
     * commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
     * Date: Thu Nov 9 20:00:05 2017 -0800
     * A commit message.
     *
     * -----------------------------------------------------------------
     *
     * For merge commits (those that have two parent commits),
     * add a line just below the first, as in
     * -----------------------------------------------------------------
     * ===
     * commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
     * Merge: 4975af1 2c1ead1
     * Date: Sat Nov 11 12:30:00 2017 -0800
     * Merged development into master.
     *
     * -----------------------------------------------------------------
     */
    @Override
    public String toString() {
        String s = "===" + "\n";
        // "commit" line
        s += "commit " + commitID + "\n";
        // ("merge" line)
        // a commit can only be merged if it has more than 1 parent (here 2 parents).
        if (parent2 != null) {
            int shortParentIDLength = 7;
            String parent1Short = parent1.substring(0, shortParentIDLength);
            String parent2Short = parent2.substring(0, shortParentIDLength);
            s += "Merge: " + parent1Short + " " + parent2Short + "\n";
        }
        // "Date" line
        SimpleDateFormat sdf = new SimpleDateFormat(
                "E MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);
        s += "Date: " + sdf.format(timeStamp) + "\n";
        // message line
        s += message + "\n";

        return s;
    }

    /** Returns the commit log history.
     * Starting at the current head commit, display information about
     * each commit backwards along the commit tree until the initial commit,
     * following the first parent commit links,
     * ignoring any second parents found in merge commits.
     *
     * @param currentCommitID the SHA1 ID of the current commit
     * @return the commit log string
     */
    public static String getCurrentCommitLog(String currentCommitID) {
        StringBuilder log = new StringBuilder();
        String commitID = currentCommitID;
        while (commitID != null) {
            Commit commitToBePresented = fromFile(commitID);
            log.append(commitToBePresented.toString()).append("\n");
            commitID = commitToBePresented.getFirstParent();
        }
        // substring to length - 1 in order to remove the last "\n"
        return log.substring(0, log.length() - 1);
    }

    /** Returns the log of all the gitlet commits.
     * Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     *
     * @return the global commit log string
     */
    public static String getGlobalCommitLog() {
        StringBuilder globalLog = new StringBuilder();
        List<String> allCommitsID = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        assert allCommitsID != null;
        for (String commitID : allCommitsID) {
            Commit commit = fromFile(commitID);
            globalLog.append(commit.toString()).append("\n");
        }
        // substring to length - 1 in order to remove the last "\n"
        return globalLog.substring(0, globalLog.length() - 1);
    }

    /** Returns all the IDs of the commits that have the given commit message.
     *
     * @param message the to-be-compared input message
     * @return the commit ID list, each of whose massage matches the given message
     */
    public static List<String> findCommitLogWithMessage(String message) {
        List<String> commitIDsToReturn = new ArrayList<>();
        List<String> allCommitsID = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        assert allCommitsID != null;
        for (String commitID : allCommitsID) {
            Commit commit = fromFile(commitID);
            if (commit.getMessage().equals(message)) {
                commitIDsToReturn.add(commit.getCommitID());
            }
        }
        return commitIDsToReturn;
    }

    @Override
    public void dump() {
        System.out.println("gitlet log of this commit:");
        System.out.println(this);
        System.out.println("map file name to blob:");
        for (String fileName : mapFileNameToBlob.keySet()) {
            System.out.printf("file name: %s to blob ID: %s%n",
                    fileName, mapFileNameToBlob.get(fileName));
        }
        System.out.println("parent1: " + parent1);
        System.out.println("parent2: " + parent2);
    }
}
