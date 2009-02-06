package de.samaflost.gwttasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FilterSet;

/**
 * Task that runs GWT's applicationCreator script.
 */
public class ApplicationCreator extends Task {

    private static final String TEMPLATE = "/de/samaflost/gwttasks/build.xml";

    private File gwtHome = null;
    private File dir = null;
    private boolean eclipse = false;
    private String className = null;
    private String outDir = null;
    private File template = null;

    public void setGwtHome(File f) {
        gwtHome = f;
    }

    public void setDir(File f) {
        dir = f;
    }

    public void setEclipse(boolean b) {
        eclipse = b;
    }

    public void setClassName(String s) {
        className = s;
    }

    public void setTemplate(File f) {
        template = f;
    }

    public void setOutDir(String s) {
        outDir = s;
    }

    public void execute() {
        if (gwtHome == null) {
            throw new BuildException("gwtHome attribute is required");
        }
        if (className == null) {
            throw new BuildException("className attribute is required");
        }

        try {
            executeApplicationCreator();
            createBuildFile();
        } catch (IOException e) {
            throw new BuildException("applicationCreator failed", e);
        }
    }
        
    private void executeApplicationCreator() throws IOException {
        Commandline cmd = new Commandline();
        String executable =
            new File(gwtHome, "applicationCreator").getAbsolutePath();
        if (Os.isFamily("windows")) {
            executable += ".cmd";
        }
        cmd.setExecutable(executable);

        if (eclipse) {
            cmd.createArgument().setValue("-eclipse");
        }

        cmd.createArgument().setValue(className);

        Execute exe = new Execute(new LogStreamHandler(this,
                                                       Project.MSG_INFO,
                                                       Project.MSG_ERR));
        exe.setAntRun(getProject());
        exe.setVMLauncher(false);
        if (dir != null) {
            exe.setWorkingDirectory(dir);
        } else {
            exe.setWorkingDirectory(getProject().getBaseDir());
        }
        exe.setCommandline(cmd.getCommandline());
        log(cmd.describeCommand(), Project.MSG_VERBOSE);

        int retval = exe.execute();
        if (Execute.isFailure(retval)) {
            throw new BuildException("applicationCreator failed with return"
                                     + " code: " + retval);
        }
    }

    private void createBuildFile() throws IOException {
        int index = className.lastIndexOf(".");
        String projectName = className;
        if (index > -1) {
            projectName = className.substring(index + 1);
        }

        index = className.indexOf(".client.");
        String classBase = className;
        if (index > -1) {
            classBase =
                className.substring(0, index) + className.substring(index + 7);
        }

        File targetDir = dir;
        if (dir == null) {
            targetDir = getProject().getBaseDir();
        }

        FilterSet fs = new FilterSet();
        fs.addFilter("PROJECT", projectName);
        fs.addFilter("GWT_HOME", gwtHome.getAbsolutePath());
        fs.addFilter("OUT", outDir != null ? outDir : "www");
        fs.addFilter("CLASS_BASE", classBase);

        InputStream in = null;
        PrintWriter out = null;
        BufferedReader bin = null; 
        try {
            if (template == null) {
                in = getClass()
                    .getResourceAsStream(TEMPLATE);
            } else {
                in = new FileInputStream(template);
            }
            bin = new BufferedReader(new InputStreamReader(in , "UTF8"));
            out = new PrintWriter(
                      new OutputStreamWriter(
                          new FileOutputStream(new File(targetDir,
                                                        "build.xml")),
                          "UTF8"));
            for (String line = bin.readLine(); line != null;
                 line = bin.readLine()) {
                out.println(fs.replaceTokens(line));
            }
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                    log("caught exception closing template stream: "
                        + e.getMessage(), Project.MSG_ERR);
                }
            } else if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log("caught exception closing template stream: "
                        + e.getMessage(), Project.MSG_ERR);
                }
            }
            if (out != null) {
                out.close();
            }
        }
    }
}