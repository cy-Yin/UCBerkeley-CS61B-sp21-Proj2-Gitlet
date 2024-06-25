package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        commitWithMerge(message, null);
    }

    /** If the commit is created by a merge, then the second parent is not null. */
    private void commitWithMerge(String message, String secondParent) {
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
        Commit newCommit = new Commit(message, head, secondParent, newCommitMapFileNameToBlob);
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
        String headCommitIDOfBranch = branchToCheckout.getCommitID();
        Commit headCommitOfBranch = Commit.fromFile(headCommitIDOfBranch);
        Commit currentCommit = Commit.fromFile(head);
        // If a working file is untracked in the current branch and
        // would be overwritten by the checkout, print the message and exit
        for (String fileName : allFiles) {
            if (headCommitOfBranch.mapFileNameToBlob.containsKey(fileName)
                    && !currentCommit.mapFileNameToBlob.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        for (String commitFileName : headCommitOfBranch.mapFileNameToBlob.keySet()) {
            String blobIDPrevVersion = headCommitOfBranch.mapFileNameToBlob.get(commitFileName);
            String fileContentPrevVersion = Blob.contentFromFile(blobIDPrevVersion);
            // overwriting the versions of the existing files
            // for files not in the working directory,  put them in the directory.
            File fileToBePutInCWD = new File(commitFileName);
            Utils.writeContents(fileToBePutInCWD, fileContentPrevVersion);
        }
        // delete the files tracked in the current branch
        // but are not present in the checked-out branch
        for (String fileInCWD : allFiles) {
            if (!headCommitOfBranch.mapFileNameToBlob.containsKey(fileInCWD)
                    && currentCommit.mapFileNameToBlob.containsKey(fileInCWD)) {
                boolean isFileInCWDDeleted = Utils.restrictedDelete(fileInCWD);
            }
        }
        StagingArea stagingArea = new StagingArea();
        activatedBranchName = branchName;
        head = headCommitIDOfBranch;
        stagingArea.clear();
        stagingArea.saveStagingArea();
        saveRepository();
    }

    /** Creates a new branch with the given name, and points it at the current head commit.
     * Like real Git, This command does NOT immediately switch to the newly created branch.
     * @code java gitlet.Main branch [branch name] */
    public void createBranch(String branchName) {
        // If a branch with the given name already exists, print the error message.
        if (Utils.plainFilenamesIn(BRANCH_DIR).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Branch newBranch = new Branch(branchName, head);
        newBranch.saveBranch();
    }

    /** Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits that were created under the branch,
     * or anything like that.
     * @code java gitlet.Main rm-branch [branch name] */
    public void removeBranch(String branchName) {
        // If a branch with the given name does not exist, abort and print the error message.
        if (!Utils.plainFilenamesIn(BRANCH_DIR).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        // If you try to remove the branch you’re currently on, abort and print the error message.
        if (branchName.equals(activatedBranchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        Branch.removeBranch(branchName);
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     * The [commit id] may be abbreviated as for checkout.
     * The staging area is cleared.
     * The command is essentially checkout of an arbitrary commit
     * that also changes the current branch head.
     * @code java gitlet.Main reset [commit id] */
    public void reset(String commitID) {
        // If no commit with the given id exists, print the message
        if (!Utils.plainFilenamesIn(COMMIT_DIR).contains(commitID)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Commit commitToReset = Commit.fromFile(commitID);
        String standardCommitID = commitToReset.getCommitID(); // the non-abbreviated commit ID
        Commit currentCommit = Commit.fromFile(head);
        // If a file in the working directory is untracked in the current branch
        // and would be overwritten by the reset (tracked by the to-be-reset commit)
        for (String fileName : allFiles) {
            if (commitToReset.mapFileNameToBlob.containsKey(fileName)
                    && !currentCommit.mapFileNameToBlob.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // Checks out all the files tracked by the given commit.
        for (String fileNameInCommitToReset : commitToReset.mapFileNameToBlob.keySet()) {
            checkoutWithCommitIDAndFileName(commitID, fileNameInCommitToReset);
        }
        // Removes tracked files that are not present in that commit.
        for (String fileName : allFiles) {
            if (currentCommit.mapFileNameToBlob.containsKey(fileName)
                    && !commitToReset.mapFileNameToBlob.containsKey(fileName)) {
                boolean isFileInCWDDeleted = Utils.restrictedDelete(fileName);
            }
        }
        head = standardCommitID;
        Branch branch = Branch.fromFile(activatedBranchName);
        branch.update(standardCommitID);
        branch.saveBranch();
        StagingArea stagingArea = new StagingArea();
        stagingArea.clear();
        stagingArea.saveStagingArea();
        saveRepository();
    }

    /** Merges files from the given branch into the current branch.
     * @code java gitlet.Main merge [branch name] */
    public void merge(String branchNameToMerge) {
        validateMerge(branchNameToMerge);

        Branch branchToMerge = Branch.fromFile(branchNameToMerge);
        String otherBranchCommitID = branchToMerge.getCommitID();
        Commit otherBranchCommit = Commit.fromFile(otherBranchCommitID);
        Commit currentHeadCommit = Commit.fromFile(head);
        String splitPointCommitID = getSplitPointCommitID(head, otherBranchCommitID,
                                                                    branchNameToMerge);
        Commit splitPointCommit = Commit.fromFile(splitPointCommitID);

        Set<String> filesToConsider = new HashSet<>();
        filesToConsider.addAll(currentHeadCommit.mapFileNameToBlob.keySet());
        filesToConsider.addAll(otherBranchCommit.mapFileNameToBlob.keySet());
        filesToConsider.addAll(splitPointCommit.mapFileNameToBlob.keySet());
        boolean isConflicted = false;
        for (String file : filesToConsider) {
            isConflicted = mergeFile(file,
                            currentHeadCommit, otherBranchCommit, splitPointCommit);
        }
        // Once files have been updated according to the above, and the split point was
        // not current branch or given branch, merge automatically commits with the log message
        String commitWithMergeMessage = "Merged " + branchNameToMerge
                                            + " into " + activatedBranchName + ".";
        commitWithMerge(commitWithMergeMessage, otherBranchCommitID);
        // Then, if the merge encountered a conflict, print the message on terminal (not log)
        if (isConflicted) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Checks the validation of the {@code merge{}} method
     *  with the input {@code branchNameToMerge}.
     *
     * @param branchNameToMerge the name of the to-be-merged branch
     */
    private void validateMerge(String branchNameToMerge) {
        StagingArea stagingArea = new StagingArea();
        // If there are staged additions or removals present, print the error message and exit.
        if (!stagingArea.getStagedForAddition().isEmpty()
                && stagingArea.getStagedForRemoval().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        // If a branch with the given name does not exist, print the error message
        if (!Utils.plainFilenamesIn(BRANCH_DIR).contains(branchNameToMerge)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        // If attempting to merge a branch with itself, print the error message
        if (branchNameToMerge.equals(activatedBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Branch branchToMerge = Branch.fromFile(branchNameToMerge);
        String otherBranchCommitID = branchToMerge.getCommitID();
        Commit otherBranchCommit = Commit.fromFile(otherBranchCommitID);
        Commit currentHeadCommit = Commit.fromFile(head);
        // If an untracked file in the current commit
        // would be overwritten or deleted by the merge, print the error message
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : allFiles) {
            if (!currentHeadCommit.mapFileNameToBlob.containsKey(fileName)
                    && otherBranchCommit.mapFileNameToBlob.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Returns the commit ID of the split point commit of the two branches, which are separately
     * the current branch ({@code currentBranchCommitID} as its head commit ID)
     * and the to-be-merged branch ({@code otherBranchCommitID} as its head commit ID)
     *
     * @param currentBranchCommitID the head commit ID of the current branch
     * @param otherBranchCommitID the head commit ID of the to-be-merged branch
     * @param otherBranchName the name of the other branch (this parameter is passed in to
     *                        deal with the case that if the split point is the current branch,
     *                        then the result of merge equals to checking out this branch)
     * @return the String which is the commit ID of the split point of these two branches
     */
    private String getSplitPointCommitID(String currentBranchCommitID,
                                         String otherBranchCommitID, String otherBranchName) {
        String splitPointCommitID = "";
        Map<String, Integer> currentCommitsGraph = buildCommitsGraph(currentBranchCommitID);
        Map<String, Integer> otherCommitsGraph = buildCommitsGraph(otherBranchCommitID);

        int minDepth = Integer.MAX_VALUE;
        for (String currentCommitID : currentCommitsGraph.keySet()) {
            int depth = currentCommitsGraph.get(currentCommitID);
            if (otherCommitsGraph.containsKey(currentCommitID) && depth < minDepth) {
                splitPointCommitID = currentCommitID;
                minDepth = depth;
            }
        }

        // If the split point is the same commit as the given branch, then we do nothing;
        // the merge is complete, and the operation ends with the message
        if (splitPointCommitID.equals(otherBranchCommitID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        // If the split point is the current branch, then the effect is to
        // check out the given branch, and the operation ends after printing the message
        if (splitPointCommitID.equals(currentBranchCommitID)) {
            checkoutWithBranchName(otherBranchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }

        return splitPointCommitID;
    }

    /** Builds the map from the commit id to the depth.
     * the "depth" is defined as follows:
     * the input commit ID has the depth 0, and its first parent and second parent (if exists)
     * both have the depth 1, with their parents have the depth 2, etc.
     * @param commitID the commit ID
     * @return the map from the commit id to the depth
     */
    private Map<String, Integer> buildCommitsGraph(String commitID) {
        Map<String, Integer> commitsGraph = new HashMap<>();
        buildCommitsGraphHelper(commitID, 0, commitsGraph);
        return commitsGraph;
    }

    /** Helper function for {@code buildCommitsGraph()} method.
     * Builds the commits map from the given commit ID with a given starting depth.
     *
     * The recursively building process has the runtime complexity of {@code O(N lgN + D)}, where
     * N is the total number of ancestor commits for the two branches and
     * D is the total amount of data in all the files under these commits.
     */
    private void buildCommitsGraphHelper(String commitID, int depth,
                                                Map<String, Integer> commitsGraph) {
        if (commitID == null) {
            return;
        }
        commitsGraph.put(commitID, depth);

        Commit commit = Commit.fromFile(commitID);
        buildCommitsGraphHelper(commit.getFirstParent(), depth + 1, commitsGraph);
        buildCommitsGraphHelper(commit.getSecondParent(), depth + 1, commitsGraph);
    }

    /** For one file, merge it from the given branch (other branch) into the current branch.
     * There are eight cases of gitlet-merging a file, while 7 of the cases have no conflicts:
     * 1. Any files that have been modified in the given branch since the split point,
     *    but not modified in the current branch since the split point should be changed
     *    to their versions in the given branch (checked out from the commit at the front
     *    of the given branch). These files should then all be automatically staged.
     * 2. Any files that have been modified in the current branch but not in the given branch
     *    since the split point should stay as they are.
     * 3. Any files that have been modified in both the current and given branch in the same way
     *    (i.e., both files now have the same content or were both removed) are left unchanged
     *    by the merge. If a file was removed from both the current and given branch, but a file
     *    of the same name is present in the working directory, it is left alone and continues
     *    to be absent (not tracked nor staged) in the merge.
     * 4. Any files that were not present at the split point and are present only in the current
     *    branch should remain as they are.
     * 5. Any files that were not present at the split point and are present only in the given
     *    branch should be checked out and staged.
     * 6. Any files present at the split point, unmodified in the current branch, and absent
     *    in the given branch should be removed (and untracked).
     * 7. Any files present at the split point, unmodified in the given branch, and absent in the
     *    current branch should remain absent.
     * 8. Any files modified in different ways in the current and given branches are in conflict.
     *    "Modified in different ways" can mean that the contents of both are changed and
     *    different from other, or the contents of one are changed and the other file is deleted,
     *    or the file was absent at the split point and has different contents in the given and
     *    current branches. In this case, replace the contents of the conflicted file and stage
     *    the result. Treat a deleted file in a branch as an empty file.
     *
     * @param file tha name of the to-be-merged file
     * @param currentHeadCommit the head commit of the current branch
     * @param otherBranchCommit the commit the other branch pointing to
     * @param splitPointCommit the latest common ancestor of the current and given branch heads
     */
    private boolean mergeFile(String file, Commit currentHeadCommit,
                                    Commit otherBranchCommit, Commit splitPointCommit) {
        boolean isConflicted = false;
        boolean isFileInCurrent = currentHeadCommit.mapFileNameToBlob.containsKey(file);
        boolean isFileInOther = otherBranchCommit.mapFileNameToBlob.containsKey(file);
        boolean isFileInSplit = splitPointCommit.mapFileNameToBlob.containsKey(file);
        String fileContentInCurrent = currentHeadCommit.getFileContentInCommit(file);
        String fileContentInOther = otherBranchCommit.getFileContentInCommit(file);
        String fileContentInSplit = splitPointCommit.getFileContentInCommit(file);
        if (isFileInCurrent && isFileInOther && isFileInSplit) {
            boolean isFileModifiedInCurrent = !fileContentInCurrent.equals(fileContentInSplit);
            boolean isFileModifiedInOther = !fileContentInOther.equals(fileContentInSplit);
            if (isFileModifiedInOther && !isFileModifiedInCurrent) { // Case 1
                /* Any files that have been modified in the given branch since the split point
                 * but not modified in the current branch since the split point
                 * should be changed to their versions in the given branch
                 * (checked out from the commit at the front of the given branch).
                 * These files should then all be automatically staged. */
                StagingArea stagingArea = new StagingArea();
                checkoutWithCommitIDAndFileName(otherBranchCommit.getCommitID(), file);
                stagingArea.addFile(file, head);
                stagingArea.saveStagingArea();
            } else if (!isFileModifiedInOther && isFileModifiedInCurrent) { // Case 2
                /* Any files that have been modified in the current branch but not
                 * in the given branch since the split point should stay as they are. */
                isConflicted = false;
            } else if (isFileModifiedInOther && isFileModifiedInCurrent) {
                if (fileContentInCurrent.equals(fileContentInOther)) { // Case 3
                    /* Any files that have been modified in both the current and given
                    branch in the same way (i.e., both files now have the same content
                    or were both removed) are left unchanged by the merge. If a file was
                    removed from both the current and given branch, but a file of the same name
                    is present in the working directory, it is left alone and continues to be
                    absent (not tracked nor staged) in the merge. */
                    isConflicted = false;
                } else { // Case 8: the file exists in (2 + 1) commits but modified differently
                    mergeFileWithConflicts(file, fileContentInCurrent, fileContentInOther);
                    isConflicted = true;
                }
            }
        } else if (!isFileInSplit && isFileInCurrent && isFileInOther
                && !fileContentInCurrent.equals(fileContentInOther)) { // Case 8
            // file was absent at split point and has different contents in 2 branches
            mergeFileWithConflicts(file, fileContentInCurrent, fileContentInOther);
            isConflicted = true;
        } else if (!isFileInSplit && !isFileInOther && isFileInCurrent) { // Case 4
            /* Any files that were not present at the split point and
             * are present only in the current branch should remain as they are. */
            isConflicted = false;
        } else if (!isFileInSplit && !isFileInCurrent && isFileInOther) { // Case 5
            /* Any files that were not present at the split point and
             * are present only in the given branch should be checked out and staged. */
            StagingArea stagingArea = new StagingArea();
            checkoutWithCommitIDAndFileName(otherBranchCommit.getCommitID(), file);
            stagingArea.addFile(file, head);
            stagingArea.saveStagingArea();
        } else if (!isFileInOther && isFileInSplit && isFileInCurrent) {
            if (fileContentInCurrent.equals(fileContentInSplit)) { // Case 6
                /* Any files present at split point, unmodified in the current branch,
                 * and absent in the given branch should be removed (and untracked). */
                remove(file);
            } else { // Case 8: file contents of given changed and file in current deleted
                mergeFileWithConflicts(file, fileContentInCurrent, fileContentInOther);
                isConflicted = true;
            }
        } else if (!isFileInCurrent && isFileInSplit && isFileInOther) {
            if (fileContentInOther.equals(fileContentInSplit)) { // Case 7
                /* Any files present at the split point, unmodified in the given branch,
                 * and absent in the current branch should remain absent. */
                isConflicted = false;
            } else { // Case 8: file contents of current changed and file in given deleted
                mergeFileWithConflicts(file, fileContentInCurrent, fileContentInOther);
                isConflicted = true;
            }
        }
        return isConflicted;
    }

    /** Deal with the case that a merge of the file have conflicts.
     *  The contents of the conflicted file is replaced with
     *  ---------------------------------------------------
     *  * <<<<<<< HEAD                                    *
     *  * contents of file in current branch              *
     *  * =======                                         *
     *  * contents of file in given branch                *
     *  * >>>>>>>                                         *
     *  ---------------------------------------------------
     *  (replacing "contents of..." with the indicated file's contents).
     *  Treat a deleted file in a branch as an empty file. Use straight concatenation here.
     *  In the case of a file with no newline at the end, you might well end up with something
     *  like this:
     *  ---------------------------------------------------
     *  * <<<<<<< HEAD                                    *
     *  * contents of file in current branch=======       *
     *  * contents of file in given branch>>>>>>>         *
     *  ---------------------------------------------------
     *
     * @param file the name of the file to be merged
     * @param fileContentInCurrent the content of this file tracked by the current branch
     * @param fileContentInOther the content of this file tracked by the other branch
     */
    private void mergeFileWithConflicts(String file,
                                    String fileContentInCurrent, String fileContentInOther) {
        String fileContentWithConflicts = "<<<<<<< HEAD" + "\n";
        if (fileContentInCurrent != null) {
            fileContentWithConflicts += fileContentInCurrent;
        }
        fileContentWithConflicts += "=======" + "\n";
        if (fileContentInOther != null) {
            fileContentWithConflicts += fileContentInOther;
        }
        fileContentWithConflicts += ">>>>>>>" + "\n";
        File fileWithConflicts = new File(file);
        Utils.writeContents(fileWithConflicts, fileContentWithConflicts);
        Blob blob = new Blob(fileContentWithConflicts);
        blob.saveBlob();
        StagingArea stagingArea = new StagingArea();
        stagingArea.addFile(file, head);
        stagingArea.saveStagingArea();
    }

    @Override
    public void dump() {
        System.out.printf("head: %s%nbranch: %s%n", head, activatedBranchName);
    }
}
