/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cn.enaium.paramorphism.decrypt.util;

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

/**
 * https://github.com/java-deobfuscator/deobfuscator
 * I am skid god
 */
public class CustomRemapper extends Remapper {

    /**
     * Map type name to the new name. Subclasses can override.
     */
    public String map(String in) {
        int lin = in.lastIndexOf('/');
        String className =  lin == -1 ? in : in.substring(lin + 1);
        if (lin == -1) {
            return map.getOrDefault(in, in);
        } else {
            String newClassName = map.getOrDefault(in, className);
            int nlin = newClassName.lastIndexOf('/');
            newClassName =  nlin == -1 ? newClassName : newClassName.substring(nlin + 1);
            return mapPackage(in.substring(0, lin)) + "/" + newClassName;
        }
    }

    public String mapPackage(String in) {
        int lin = in.lastIndexOf('/');
        if (lin != -1) {
            String originalName = in.substring(lin + 1);
            String parentPackage = in.substring(0, lin);
            String newPackageName = packageMap.getOrDefault(in, originalName);
            int nlin = newPackageName.lastIndexOf('/');
            newPackageName =  nlin == -1 ? newPackageName : newPackageName.substring(nlin + 1);
            return mapPackage(parentPackage) + "/" + newPackageName;
        } else {
            return packageMap.getOrDefault(in, in);
        }
    }

    public boolean mapPackage(String oldPackage, String newPackage) {
        if (!packageMapReversed.containsKey(newPackage) && !packageMap.containsKey(oldPackage)) {
            packageMapReversed.put(newPackage, oldPackage);
            packageMap.put(oldPackage, newPackage);
            return true;
        }
        return false;
    }

    private Map<String, String> map = new HashMap<>();
    private Map<String, String> mapReversed = new HashMap<>();

    private Map<String, String> packageMap = new HashMap<>();
    private Map<String, String> packageMapReversed = new HashMap<>();

    public boolean map(String old, String newName) {
        if (mapReversed.containsKey(newName)) {
            return false;
        }
        map.put(old, newName);
        mapReversed.put(newName, old);
        return true;
    }

}
