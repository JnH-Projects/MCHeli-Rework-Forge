package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import java.lang.reflect.Method;

/**
 * Tells Iris which shader program draws our own render pipelines. Iris only knows the vanilla
 * pipelines and draws any other with a program whose vertex layout does not match, which throws
 * the vertices far off: the glows turn into long coloured lines running to the horizon. Iris is
 * optional, so its API is looked up by reflection instead of compiled against.
 */
final class IrisCompat {
    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String PROGRAM = "net.irisshaders.iris.api.v0.IrisProgram";

    private IrisCompat() {
    }

    /**
     * Assigns {@code pipeline} to the Iris program named {@code program}, such as
     * {@code EMISSIVE_ENTITIES}. Does nothing without Iris.
     *
     * @return whether Iris took it
     */
    static boolean assign(RenderPipeline pipeline, String program) {
        Class<?> api;
        try {
            api = Class.forName(API);
        } catch (ClassNotFoundException absent) {
            MCHeli.LOGGER.debug("Iris is not installed, {} keeps its own shader", pipeline.getLocation());
            return false;
        }
        try {
            Class<?> programs = Class.forName(PROGRAM);
            Object instance = api.getMethod("getInstance").invoke(null);
            Object target = programs.getField(program).get(null);
            Method assign = api.getMethod("assignPipeline", RenderPipeline.class, programs);
            assign.invoke(instance, pipeline, target);
            MCHeli.LOGGER.debug("Iris draws {} with its {} program", pipeline.getLocation(), program);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            // An Iris without this API: glows may look wrong under a shader pack, nothing worse.
            MCHeli.LOGGER.warn("Could not hand {} to Iris, glows may look wrong with shaders: {}", pipeline.getLocation(), e.toString());
            return false;
        }
    }
}
