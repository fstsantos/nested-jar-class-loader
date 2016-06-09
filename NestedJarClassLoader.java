import java.io.FileInputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class NestedJarClassLoader extends ClassLoader {

	private static final Logger logger = LoggerFactory.getLogger(NestedJarClassLoader.class);
	
    public NestedJarClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Loads all classes from nested jar.
     * 
     * @param basePackage Base package name, ex: "org.package.example"
     * @param jarName The name of the nested jar, it doesn't have to be the exact name of the jar,
     * 		          just a substring, ex: "name-of-jar"
     */
    public void loadSwitchTags(String basePackage, String jarName) {
        String basePackageName = basePackage.replace(".", "/");
    	String strPath = "classpath*:" + basePackageName + "/**/*";
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(strPath);

        for (Resource resource : resources) {
            String uri = resource.getURI().toString();
            String topJar = uri.substring(uri.indexOf("file:") + 5, uri.indexOf("!"));
            
            JarInputStream topJarStream = new JarInputStream(new FileInputStream(topJar));
            JarEntry topEntry = null;
            while ((topEntry = topJarStream.getNextJarEntry()) != null) {
                if (topEntry.getName().contains(jarName)) {
                    JarInputStream nestedJarStream = new JarInputStream(topJarStream);
                    JarEntry nestedEntry = null;
                    
                    while ((nestedEntry = nestedJarStream.getNextJarEntry()) != null) {
                        if (nestedEntry.getName().contains(basePackageName) && 
                        		nestedEntry.getName().contains(".class") &&
                                !nestedEntry.getName().contains("$")) {
                            
                            int length = (int) nestedEntry.getSize();
                            byte[] classBytes = new byte[length];
                            int n = 0;
                            while (n < length) {
                                int r = nestedJarStream.read(classBytes, n, classBytes.length - n);
                                if (r > 0) {
                                    n += r;
                                }
                            }
                            
                            String className = nestedEntry.getName().replace(".class", "").replace("/", ".");
                            Class<?> clazz = defineClass(className, classBytes, 0, classBytes.length);

                            logger.info(String.format("Loaded class %s from nested jar %s", clazz, jarName));
                        }
                    }
                    nestedJarStream.close();
                }
            }
            topJarStream.close();
			break;
        }
     }

}
