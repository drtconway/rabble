package org.petermac.rabble;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(description = "build and manipulate reports using exemplars-as-templates",
         name = "rabble",
         version = "rabble 1.0.0",
         subcommands = {
            RabbleCliTest.class
         })
class RabbleCli implements Callable<Integer> {

    @Option(names = {"-V", "--version"},
            versionHelp = true,
            description = "display version information")
    public boolean versionRequested;

    @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "display usage information")
    public boolean helpRequested;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new RabbleCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Actual main function
        return 0; // success!
    }
}
