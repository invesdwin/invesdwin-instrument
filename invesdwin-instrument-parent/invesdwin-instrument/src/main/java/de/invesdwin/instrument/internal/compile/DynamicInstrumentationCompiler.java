package de.invesdwin.instrument.internal.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import de.invesdwin.instrument.DynamicInstrumentationProperties;

/**
 * Extracted from: https://github.com/kilim/kilim/blob/master/src/kilim/tools/Javac.java
 *
 * @author subes
 *
 */
//CHECKSTYLE:OFF
//@NotThreadSafe
public final class DynamicInstrumentationCompiler {

    private DynamicInstrumentationCompiler() {
    }

    /**
     * Given a list of file-scope java code (equivalent to a .java file, including package and import declarations),
     * compile() invokes javac to compile them, produce classfiles and return a list of <className, byte[]> pairs.
     * 
     * compile() dumps the source strings into their respective files, has javac compile them, then reads back the
     * equivalent class files. The name of the source file is gleaned from the string itself; a string containing
     * "public class Foo" is stored in tmpDir/Foo.java (where tmpDir is a temporary directory that's deleted after the
     * compilation), and if no public class or interface is found, the name of the first class in the string is used.
     * 
     * Note that the list of returned classes may be larger than list of sources
     * 
     * Note: the java compiler api is ill-defined and this class should not be considered production. specifically, the
     * classpath appears to depend on the execution environment, eg command line maven vs IDE vs the java command line
     * 
     * @param srcCodes
     *            . List of strings.
     * @return List<className,byte[]>. className is fully qualified, and byte[] contains the bytecode of the class.
     * @throws IOException
     */
    public static List<DynamicInstrumentationClassInfo> compile(final List<String> srcCodes) throws IOException {

        final List<SourceInfo> srcInfos = getSourceInfos(srcCodes);

        final File rootDir = getTmpDir(); // something like "/tmp/kilim$2348983948"

        final File classDir = new File(rootDir.getAbsolutePath() + File.separatorChar + "classes");
        classDir.mkdir(); // "<rootDir>/classes"

        /**
         * the compiler classpath appears to depend on the class.path system variable which changes depending on the
         * execution environment, eg command line maven vs command line java vs IDE vs ant to limit this dependence,
         * generate a classpath from the class loader urls instead
         */
        final String cp = getClassPath(null, null).join();

        final ArrayList<String> args = new ArrayList();
        add(args, "-d", classDir.getAbsolutePath());
        if (!cp.isEmpty()) {
            add(args, "-cp", cp);
        }
        for (final SourceInfo srci : srcInfos) {
            final String name = rootDir.getAbsolutePath() + File.separatorChar + srci.className + ".java";
            writeFile(new File(name), srci.srcCode.getBytes());
            args.add(name);
        }
        final String[] arguments = args.toArray(new String[0]);

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, arguments);

        final List<DynamicInstrumentationClassInfo> ret = new ArrayList<DynamicInstrumentationClassInfo>();
        addClasses(ret, "", classDir);
        deleteDir(rootDir);
        return ret;
    }

    static void add(final ArrayList<String> list, final String... vals) {
        for (final String val : vals) {
            list.add(val);
        }
    }

    /**
     * get the class path comprising the paths of URL class loaders ancestors
     *
     * @param start
     *            start with the class loader that loaded this object, or null for this method's class
     * @param end
     *            the last classloader to consider, or null to include everything up to but not including the system
     *            class loader
     * @return the URLs
     */
    public static ClassPath getClassPath(final Class start, final ClassLoader end) {
        final ClassPath result = new ClassPath();
        final ClassLoader sys = end == null ? ClassLoader.getSystemClassLoader() : end.getParent();
        ClassLoader cl = (start == null ? DynamicInstrumentationCompiler.class : start).getClassLoader();
        // FIXME::rhetorical - what order should the classpath be ?
        //   the reality is that calling the compiler cannot be 100% robust
        //   so this detail is likely insignificant
        for (; cl != null & cl != sys; cl = cl.getParent()) {
            if (cl instanceof java.net.URLClassLoader) {
                for (final java.net.URL url : ((java.net.URLClassLoader) cl).getURLs()) {
                    result.add(url.getPath());
                }
            }
        }
        return result;
    }

    /** a collection of class path elements */
    public static class ClassPath extends ArrayList<String> {
        /** get the command-line-style classpath string */
        public String join() {
            String cp = "";
            for (final String url : this) {
                cp += (cp.isEmpty() ? "" : File.pathSeparator) + url;
            }
            return cp;
        }
    }

    private static List<SourceInfo> getSourceInfos(final List<String> srcCodes) {
        final List<SourceInfo> srcInfos = new ArrayList<SourceInfo>(srcCodes.size());
        for (final String srcCode : srcCodes) {
            srcInfos.add(getSourceInfo(srcCode));
        }
        return srcInfos;
    }

    private static final Pattern publicClassNameRegexp = Pattern.compile("public +(?:class|interface) +(\\w+)");
    private static final Pattern classNameRegexp = Pattern.compile("(?:class|interface) +(\\w+)");

    private static SourceInfo getSourceInfo(final String srcCode) {
        Matcher m = publicClassNameRegexp.matcher(srcCode);
        if (m.find()) {
            return new SourceInfo(m.group(1), srcCode);
        } else {
            m = classNameRegexp.matcher(srcCode);
            if (m.find()) {
                return new SourceInfo(m.group(1), srcCode);
            } else {
                throw new IllegalArgumentException(
                        "No class or interface definition found in src: \n'" + srcCode + "'");
            }
        }
    }

    private static File getTmpDir() throws IOException {
        final String name = DynamicInstrumentationProperties.PROCESS_TEMP_DIRECTORY + "/"
                + DynamicInstrumentationCompiler.class.getSimpleName();
        final File rootDir = new File(name);
        if (!rootDir.mkdir()) {
            throw new IOException("Unable to make tmp directory " + rootDir.getAbsolutePath());
        }
        return rootDir;
    }

    private static void deleteDir(final File rootDir) {
        for (final File f : rootDir.listFiles()) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else {
                if (!f.delete()) {
                    System.err.println("Unable to delete " + f.getAbsolutePath());
                }
            }
        }
        if (!rootDir.delete()) {
            System.err.println("Unable to delete " + rootDir.getAbsolutePath());
        }
    }

    private static void addClasses(final List<DynamicInstrumentationClassInfo> ret, final String pkgName,
            final File dir) throws IOException {
        for (final File f : dir.listFiles()) {
            final String fname = f.getName();
            if (f.isDirectory()) {
                final String qname = pkgName + fname + ".";
                addClasses(ret, qname, f);
            } else if (fname.endsWith(".class")) {
                final String qname = pkgName + fname.substring(0, fname.length() - 6);
                ret.add(new DynamicInstrumentationClassInfo(qname, readFile(f)));
            } else {
                System.err.println("Unexpected file : " + f.getAbsolutePath());
            }
        }
    }

    private static byte[] readFile(final File f) throws IOException {
        int len = (int) f.length();
        final byte[] buf = new byte[len];
        final FileInputStream fis = new FileInputStream(f);
        int off = 0;
        while (len > 0) {
            final int n = fis.read(buf, off, len);
            if (n == -1) {
                throw new IOException("Unexpected EOF reading " + f.getAbsolutePath());
            }
            off += n;
            len -= n;
        }
        return buf;
    }

    private static void writeFile(final File f, final byte[] srcCode) throws IOException {
        final FileOutputStream fos = new FileOutputStream(f);
        fos.write(srcCode);
        fos.close();
    }

    private static class SourceInfo {
        public SourceInfo(final String nm, final String code) {
            className = nm;
            srcCode = code;
        }

        public String className;
        public String srcCode;
    }
}