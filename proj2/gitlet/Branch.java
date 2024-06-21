package gitlet;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/** Represents a gitlet branch object.
 *  Combinations of the name of the branch
 *  and the current commit ID which the branch pointer points at.
 */
public class Branch implements Serializable, Dumpable {
    @Serial
    private static final long serialVersionUID = -5094230405275657934L;
    /** The name of this branch. */
    private String name;
    /** The commit ID this branch pointing at. */
    private String commitID;

    public Branch(String name, String commitID) {
        this.name = name;
        this.commitID = commitID;
    }

    /** Returns the name of this branch. */
    public String getName() {
        return name;
    }

    /** Returns the commit ID this branch pointing at. */
    public String getCommitID() {
        return commitID;
    }

    /**
     * Reads in and deserializes a branch from a file with the branch name in BRANCH_DIR.
     *
     * @param branchName the name of the branch to load
     * @return the branch
     * @source modified from lab6.Dog.fromFile
     */
    public static Branch fromFile(String branchName) {
        File branchFile = Utils.join(Repository.BRANCH_DIR, branchName);
        if (!branchFile.exists()) {
            return null;
        }
        return Utils.readObject(branchFile, Branch.class);
    }

    /** Saves a branch to a file for future use.
     * @source modified from lab6.Dog.saveDog */
    public void saveBranch() {
        if (!Repository.BRANCH_DIR.exists()) {
            Repository.BRANCH_DIR.mkdir();
        }
        File saveFile = Utils.join(Repository.BRANCH_DIR, name);
        Utils.writeObject(saveFile, this);
    }

    /** When a new commit has been created, this branch pointer must be updated. */
    public void update(String newCommitID) {
        this.commitID = newCommitID;
        saveBranch();
    }

    /** Removes this branch file in the repository. */
    public static void removeBranch(String branchName) {
        File branchToRemove = Utils.join(Repository.BRANCH_DIR, branchName);
        if (!branchToRemove.exists()) {
            return;
        }
        branchToRemove.delete();
    }

    @Override
    public void dump() {
        System.out.printf("name: %s%ncommitID: %s%n", name, commitID);
    }
}
