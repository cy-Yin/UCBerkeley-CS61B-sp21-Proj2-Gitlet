package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author C. Yin
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *
     *  Commands contain {init, add, commit, rm, log, global-log,
     *           find, status, checkout, branch, rm-branch, reset, merge} etc.
     *  Details can be obtained from the switch-case in {@code gitlet.Main.main} method
     *  and <a href="https://sp21.datastructur.es/materials/proj/proj2/proj2#the-commands">
     *      proj2#the-commands</a>
     *
     * All persistent data should be stored in a ".gitlet"
     * directory in the current working directory.
     *
     * .gitlet/
     *      - commits/ -- folder containing all the persistent data for commits
     *          - commit1_SHA1 -- commit1 file (named as its SHA1 ID, containing metadata)
     *          - commit2_SHA1 -- commit2 file (similar to commit1_SHA1)
     *          - ...
     *      - branches/ -- folder containing all the persistent data for branches
     *          - master -- named as "master", the commit SHA1 ID string as its contents
     *          - branch1 -- similar to "master"
     *          - branch2 -- similar to "master"
     *          - ...
     *      - blobs/ -- folder containing all the persistent data for blobs
     *          - blob1_SHA1 -- named as blob1's SHA1_ID, containing file contents of blob1
     *          - blob2_SHA1 -- similar to blob1_SHA1
     *          - ...
     *      - repository -- file containing the current head and branch pointers
     *      - stagingArea -- file containing the staging state, including addition and removal
     */
    public static void main(String[] args) {
        if (args.length == 0) { // what if args is empty?
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        Repository repository = new Repository();
        String firstArg = args[0];
        // handle the remote commands: when first arg ends with "-remote", such as "add-remote"
//        if (firstArg.equals("push") || firstArg.equals("fetch") || firstArg.equals("pull")
//            || (firstArg.length() >= 7
//                && firstArg.substring(firstArg.length() - 7).equals("-remote"))) {
//            handleRemoteArgs(args, repository);
//            return;
//        }
        switch (firstArg) {
            case "init": // handle the `init` command: java gitlet.Main init
                validateNumAndFormatArgs(args, 1);
                repository.init();
                break;
            case "add": // handle the `add` command: java gitlet.Main add [file name]
                validateNumAndFormatArgs(args, 2);
                repository.add(args[1]);
                break;
            case "commit": // handle the `commit` command: java gitlet.Main commit [message]
                validateNumAndFormatArgs(args, 2);
                String message = args[1];
                repository.commit(message);
                break;
            case "rm": // handle the `rm` command: java gitlet.Main rm [file name]
                validateNumAndFormatArgs(args, 2);
                String fileNameToBeRemoved = args[1];
                repository.remove(fileNameToBeRemoved);
                break;
            case "log": // handle the `log` command: java gitlet.Main log
                validateNumAndFormatArgs(args, 1);
                repository.printLog();
                break;
            case "global-log": // handle the `global-log` command: java gitlet.Main global-log
                validateNumAndFormatArgs(args, 1);
                repository.printGlobalLog();
                break;
            case "find": // handle the `find` command: java gitlet.Main find [commit message]
                validateNumAndFormatArgs(args, 2);
                String messageToBeFound = args[1];
                repository.printFoundIDs(messageToBeFound);
                break;
            case "status": // handle the `status` command: java gitlet.Main status
                validateNumAndFormatArgs(args, 1);
                repository.printStatus();
                break;
            case "checkout": // handle the `checkout` command
                handleCheckoutArg(args, repository);
                break;
            case "branch": // handle the `branch` command: java gitlet.Main branch [branch name]
                validateNumAndFormatArgs(args, 2);
                String branchNameToCreate = args[1];
                repository.createBranch(branchNameToCreate);
                break;
            case "rm-branch": // handle rm-branch command: java gitlet.Main rm-branch [branch name]
                validateNumAndFormatArgs(args, 2);
                String branchNameToRemove = args[1];
                repository.removeBranch(branchNameToRemove);
                break;
            case "reset": // handle the `reset` command: java gitlet.Main reset [commit id]
                validateNumAndFormatArgs(args, 2);
                String commitID = args[1];
                repository.reset(commitID);
                break;
            case "merge": // handle the `merge` command: java gitlet.Main merge [branch name]
                validateNumAndFormatArgs(args, 2);
                String branchNameToMerge = args[1];
                repository.merge(branchNameToMerge);
                break;
            default: // if the user inputs a command that does not exist
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
        return;
    }

    /** Handles the checkout command.
     * The checkout command has three cases:
     * 1. {@code java gitlet.Main checkout -- [file name]}
     * 2. {@code java gitlet.Main checkout [commit id] -- [file name]}
     * 3. {@code java gitlet.Main checkout [branch name]}
     */
    private static void handleCheckoutArg(String[] args, Repository repository) {
        if (args.length == 3) {
            // java gitlet.Main checkout -- [file name]
            validateNumAndFormatArgs(args, 3);
            String fileName = args[2];
            repository.checkoutWithFileName(fileName);
        } else if (args.length == 4) {
            // java gitlet.Main checkout [commit id] -- [file name]
            validateNumAndFormatArgs(args, 4);
            String commitID = args[1];
            String fileName = args[3];
            repository.checkoutWithCommitIDAndFileName(commitID, fileName);
        } else if (args.length == 2) {
            // java gitlet.Main checkout [branch name]
            validateNumAndFormatArgs(args, 2);
            String branchName = args[1];
            repository.checkoutWithBranchName(branchName);
        }
    }

    /** Handles the 5 gitlet remote commands (extra credits) as follows:
     *  {@code add-remote}, {@code rm-remote}, {@code push}, {@code fetch} and {@code pull}.
     *
     *  * one can put the remote commands in the following switch-cases in {@code main()} method,
     *  but the cs61b sp21 check style demands that no method may be longer than 80 lines,
     *  so I just put the last 5 remote commands in this {@code handleRemoteArgs()} method. *
     */
    private static void handleRemoteArgs(String[] args, Repository repository) {
        // TODO
    }

    /**
     * Checks the number and format of arguments versus the expected ones,
     * throws a RuntimeException or prints the error message and exits
     * if they do not match.
     *
     * @param args Argument array from command line
     * @param n Number of expected arguments
     *
     * @source lab6.Main.validateNumArgs
     */
    public static void validateNumAndFormatArgs(String[] args, int n) {
        // if the user inputs a command with the wrong number of operands
        if (args.length != n) {
            throw new RuntimeException("Incorrect operands.");
        }

        // if the user inputs a command with the wrong format of operands
        // for example, "checkout"
        String firstArg = args[0];
        if (firstArg.equals("checkout")) {
            if (args.length == 3 && !args[1].equals("--")) {
                // java gitlet.Main checkout -- [file name]
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            if (args.length == 4 && !args[2].equals("--")) {
                // java gitlet.Main checkout [commit id] -- [file name]
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        }

        /* If a user inputs a command that requires being in an
         * initialized Gitlet working directory
         * (i.e., one containing a .gitlet subdirectory),
         * but is not in such a directory,
         * print the message "Not in an initialized Gitlet directory."
         */
        if (!args[0].equals("init") && !Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
