package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  Implements several helper methods for the main class to call.
 *  such as `java gitlet.Main {init, add, commit, rm, log, global-log,
 *                          find, status, checkout, branch, rm-branch, reset, merge}` etc.
 *  @author C. Yin
 */
public class Repository implements Serializable, Dumpable {
    @Serial
    private static final long serialVersionUID = 5888947824844088329L;
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .gitlet/commits directory. */
    public static final File COMMIT_DIR = join(GITLET_DIR, "commits");
    /** The .gitlet/branches directory. */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branches");
    /** The .gitlet/blobs directory. */
    public static final File BLOB_DIR = join(GITLET_DIR, "blobs");
    /** The head pointer. */
    private String head;
    /** The current activated branch pointer. */
    private String activatedBranchName;

    /** A repository instance (and the repo file) only contains two pointers,
     *  head and the activated branch. */
    public Repository() {
        if (GITLET_DIR.exists()) {
            File repository = join(GITLET_DIR, "repo");
            if (repository.exists()) {
                Repository repo = readObject(repository, Repository.class);
                this.head = repo.head;
                this.activatedBranchName = repo.activatedBranchName;
            }
        }
    }

    /** Saves a repository to a file for future use.
     * @source modified from lab6.Dog.saveDog */
    public void saveRepository() {
        File saveFile = join(GITLET_DIR, "repo");
        writeObject(saveFile, this);
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * @code java gitlet.Main init */
    public void init() {
        /* If there is already a Gitlet version-control system
         * in the current directory, it should abort.
         * It should NOT overwrite the existing system with a new one. */
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists"
                    + " in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        // initialize the commit.
        COMMIT_DIR.mkdir();
        Commit initCommit = new Commit();
        initCommit.saveCommit();
        // initialize the branch.
        BRANCH_DIR.mkdir();
        Branch branch = new Branch("master", initCommit.getCommitID());
        branch.saveBranch();
        // initialize the repository.
        head = initCommit.getCommitID();
        activatedBranchName = "master";
        this.saveRepository();
        // initialize the staging area.
        StagingArea stagingArea = new StagingArea();
        stagingArea.saveStagingArea();
        // initialize the blobs.
        BLOB_DIR.mkdir();
    }

    /** Adds a copy of the file as it currently exists to the staging area
     * @code java gitlet.Main add [file name] */
    public void add(String fileName) {
        File fileToBeAdded = new File(fileName);
        if (!fileToBeAdded.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        StagingArea stagingArea = new StagingArea();
        stagingArea.addFile(fileName, head);
        stagingArea.saveStagingArea();
    }

    /** Saves a snapshot of tracked files in the current commit and staging area
     * so that they can be restored at a later time, creating a new commit.
     * @code java.gitlet.Main commit [message] */
    public void commit(String message) {
        StagingArea stagingArea = new StagingArea();
        if (stagingArea.getStagedForAddition().isEmpty()
                && stagingArea.getStagedForRemoval().isEmpty()) {
            // If no files have been staged, abort.
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.isEmpty()) { // Every commit must have a non-blank message.
            System.out.println("Please enter a commit message.");
        }

        Commit currentCommit = Commit.fromFile(head);
        Map<String, String> newCommitMapFileNameToBlob = currentCommit.mapFileNameToBlob;
        for (String fileToBeRemoved : stagingArea.getStagedForRemoval()) {
            newCommitMapFileNameToBlob.remove(fileToBeRemoved);
        }
        for (String fileToBeAddedOrModified : stagingArea.getStagedForAddition().keySet()) {
            newCommitMapFileNameToBlob.put(fileToBeAddedOrModified,
                    stagingArea.getStagedForAddition().get(fileToBeAddedOrModified));
        }
        Commit newCommit = new Commit(message, head, null, newCommitMapFileNameToBlob);
        head = newCommit.getCommitID();
        newCommit.saveCommit();
        Branch branch = Branch.fromFile(activatedBranchName);
        branch.update(newCommit.getCommitID());
        stagingArea.clear();
        stagingArea.saveStagingArea();
        saveRepository();
    }

    /** "gitletly" remove the file:
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from the working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     * @code java gitlet.Main rm [file name] */
    public void remove(String fileName) {
        Commit currentCommit = Commit.fromFile(head);
        StagingArea stagingArea = new StagingArea();
        // Failure cases: the file is neither staged nor tracked by the head commit
        if (!currentCommit.mapFileNameToBlob.containsKey(fileName)
                && !stagingArea.getStagedForAddition().containsKey(fileName)
                && !stagingArea.getStagedForRemoval().contains(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        stagingArea.removeFile(fileName, currentCommit.getCommitID());
        stagingArea.saveStagingArea();
    }

    /** Prints out the commit log staring at the current head commit.
     * @code java gitlet.Main log */
    public void printLog() {
        String commitLog = Commit.getCurrentCommitLog(head);
        System.out.println(commitLog);
    }

    /** Prints out the global log of all the gitlet commits.
     * @code java gitlet.Main global-log */
    public void printGlobalLog() {
        String globalCommitLog = Commit.getGlobalCommitLog();
        System.out.println(globalCommitLog);
    }

    /** Prints out the ids of all commits that have the given commit message, one per line.
     * If there are multiple such commits, it prints the ids out on separate lines.
     * @code java gitlet.Main find [commit message] */
    public void printFoundIDs(String message) {
        List<String> foundCommitIDs = Commit.findCommitLogWithMessage(message);
        // If no such commit exists, prints the error message.
        if (foundCommitIDs.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        for (String commitID : foundCommitIDs) {
            System.out.println(commitID);
        }
    }

    /** Prints out what branches currently exist and the status of all the files.
     * For example status
     * --------------------------------------------------------
     * === Branches ===
     * *master
     * other-branch
     *
     * === Staged Files ===
     * wug.txt
     * wug2.txt
     *
     * === Removed Files ===
     * goodbye.txt
     *
     * === Modifications Not Staged For Commit ===
     * junk.txt (deleted)
     * wug3.txt (modified)
     *
     * === Untracked Files ===
     * random.stuff
     *
     * --------------------------------------------------------
     * @code java gitlet.Main status */
    public void printStatus() {
        StagingArea stagingArea = new StagingArea();
        // prints out the branches info
        System.out.println("=== Branches ===");
        for (String branchName : Utils.plainFilenamesIn(BRANCH_DIR)) {
            if (branchName.equals(activatedBranchName)) { // marks current branch with *
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();
        // prints out the staged files info
        System.out.println("=== Staged Files ===");
        printStagedFilesWithOrder(stagingArea);
        System.out.println();
        // prints out the staged files info
        System.out.println("=== Removed Files ===");
        printRemovedFilesWithOrder(stagingArea);
        System.out.println();
        // prints out the modified but not staged for commit files info
        System.out.println("=== Modifications Not Staged For Commit ===");
        printModifiedNotStagedFilesWithOrder(stagingArea);
        System.out.println();
        // prints out the untracked files info
        System.out.println("=== Untracked Files ===");
        printUntrackedFilesWithOrder(stagingArea);
        System.out.println();
    }

    /** Prints the staged files with lexicographic order
     * in the stagedForAddition in stagingArea. */
    private void printStagedFilesWithOrder(StagingArea stagingArea) {
        String[] stagedFiles = stagingArea.getStagedForAddition().keySet().toArray(new String[0]);
        Arrays.sort(stagedFiles);
        for (String stagedFile : stagedFiles) {
            System.out.println(stagedFile);
        }
    }

    /** Prints the staged files with lexicographic order
     * in the stagedForAddition in stagingArea. */
    private void printRemovedFilesWithOrder(StagingArea stagingArea) {
        String[] removedFiles = stagingArea.getStagedForRemoval().toArray(new String[0]);
        Arrays.sort(removedFiles);
        for (String removeFile : removedFiles) {
            System.out.println(removeFile);
        }
    }

    /* =============================================================================
     * Two methods (modifications not staged and untracked files) as follows are extra credit.
     * Feel free to leave them blank (leaving just the headers).
     */

    /** Prints the modified but not staged files with lexicographic order. */
    private void printModifiedNotStagedFilesWithOrder(StagingArea stagingArea) {
        List<String> modifiedNotStagedFilesWithDescription = new ArrayList<>();
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Commit currentCommit = Commit.fromFile(head);
        // A file in the working directory is "modified but not staged" if it is
        for (String fileName : allFiles) {
            File file = new File(fileName);
            Blob currentblob = new Blob(Utils.readContentsAsString(file));
            // 1. Tracked in the current commit, changed in the working directory, but not staged
            if (currentCommit.mapFileNameToBlob.containsKey(fileName)
                && !currentCommit.mapFileNameToBlob.get(fileName).equals(currentblob.getBlobID())
                && !stagingArea.getStagedForAddition().containsKey(fileName)) {
                modifiedNotStagedFilesWithDescription.add(fileName + " (modified)");
            } else if (stagingArea.getStagedForAddition().containsKey(fileName) 
                && !currentblob.getBlobID().equals(
                        stagingArea.getStagedForAddition().get(fileName))) {
                // 2. Staged for addition, but with different contents than in working directory
                modifiedNotStagedFilesWithDescription.add((fileName + " (modified)"));
            }
        }
        // 3. Staged for addition, but deleted in the working directory
        for (String fileName : stagingArea.getStagedForAddition().keySet()) {
            if (!allFiles.contains(fileName)) {
                modifiedNotStagedFilesWithDescription.add((fileName + " (deleted)"));
            }
        }
        // 4. Not staged for removal, but tracked in the current commit
        // and deleted from the working directory
        for (String fileName : currentCommit.mapFileNameToBlob.keySet()) {
            if (!stagingArea.getStagedForRemoval().contains(fileName)
                    && !allFiles.contains(fileName)) {
                modifiedNotStagedFilesWithDescription.add((fileName + " (deleted)"));
            }
        }

        String[] modifiedNotStagedFilesWithDescriptionArray =
                modifiedNotStagedFilesWithDescription.toArray(new String[0]);
        Arrays.sort(modifiedNotStagedFilesWithDescriptionArray);
        for (String modifiedFile : modifiedNotStagedFilesWithDescriptionArray) {
            System.out.println(modifiedFile);
        }
    }

    /** Prints the untracked files with lexicographic order.
     * Files present in the working directory but neither staged for addition nor tracked.
     * This includes files that have been staged for removal,
     * but then re-created without Gitlet’s knowledge. */
    private void printUntrackedFilesWithOrder(StagingArea stagingArea) {
        List<String> untrackedFiles = new ArrayList<>();
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Commit currentCommit = Commit.fromFile(head);
        for (String fileName : allFiles) {
            if (!currentCommit.mapFileNameToBlob.containsKey(fileName)
                    && !stagingArea.getStagedForAddition().containsKey(fileName)) {
                untrackedFiles.add(fileName);
            } else if (stagingArea.getStagedForRemoval().contains(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        String[] untrackedFilesArray = untrackedFiles.toArray(new String[0]);
        Arrays.sort(untrackedFilesArray);
        for (String untrackedFile : untrackedFilesArray) {
            System.out.println(untrackedFile);
        }
    }

    /* ============================================================================= */

    /** Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * @code java gitlet.Main checkout -- [file name] */
    public void checkoutWithFileName(String fileName) {
        checkoutWithCommitIDAndFileName(head, fileName);
    }

    /** Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * @code java gitlet.Main checkout [commit id] -- [file name] */
    public void checkoutWithCommitIDAndFileName(String commitID, String fileName) {
        Commit commitToCheckout = Commit.fromFile(commitID);
        File fileToCheckout = new File(fileName);
        // if no commit with the given id exists
        if (commitToCheckout == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        // if the file does not exist in the given commit
        if (!commitToCheckout.mapFileNameToBlob.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobIDOfFilePrevVersion = commitToCheckout.mapFileNameToBlob.get(fileName);
        String fileContentPrevVersion = Blob.contentFromFile(blobIDOfFilePrevVersion);
        Utils.writeContents(fileToCheckout, fileContentPrevVersion);
    }

    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions of the files
     * that are already there if they exist. Also, at the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not present
     * in the checked-out branch are deleted. The staging area is cleared,
     * unless the checked-out branch is the current branch.
     * @code java gitlet.Main checkout [branch name] */
    public void checkoutWithBranchName(String branchName) {
        // if no branch with that name exist
        if (!Utils.plainFilenamesIn(BRANCH_DIR).contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        // if that branch is the current branch
        if (branchName.equals(activatedBranchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Branch branchToCheckout = Branch.fromFile(branchName);
        String latestCommitIDOfBranch = branchToCheckout.getCommitID();
        Commit latestCommitOfBranch = Commit.fromFile(latestCommitIDOfBranch);
        Commit currentCommit = Commit.fromFile(head);
        StagingArea stagingArea = new StagingArea();
        for (String commitFileName : latestCommitOfBranch.mapFileNameToBlob.keySet()) {
            String fileContentPrevVersion =
                    latestCommitOfBranch.mapFileNameToBlob.get(commitFileName);
            if (allFiles.contains(commitFileName)) {
                // overwriting the versions of the existing files

                // If a working file is untracked in the current branch and
                // would be overwritten by the checkout, print the message and exit
                if (!currentCommit.mapFileNameToBlob.containsKey(commitFileName)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    System.exit(0);
                }

                Utils.writeContents(new File(commitFileName), fileContentPrevVersion);
            } else { // for files not in the working directory,  put them in the directory.
                File fileToBePutInCWD = new File(commitFileName);
                Utils.writeContents(fileToBePutInCWD, fileContentPrevVersion);
            }
        }
        // delete the files tracked in the current branch
        // but are not present in the checked-out branch
        for (String fileInCWD : allFiles) {
            if (!latestCommitOfBranch.mapFileNameToBlob.containsKey(fileInCWD)
                    && currentCommit.mapFileNameToBlob.containsKey(fileInCWD)) {
                boolean isFileInCWDDeleted = Utils.restrictedDelete(fileInCWD);
            }
        }
        activatedBranchName = branchName;
        head = latestCommitIDOfBranch;
        stagingArea.clear();
        stagingArea.saveStagingArea();
    }

    @Override
    public void dump() {
        System.out.printf("head: %s%nbranch: %s%n", head, activatedBranchName);
    }
}
