/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.dag.AbstractNode;
import static org.terasology.rendering.opengl.DefaultDynamicFBOs.READ_ONLY_GBUFFER;
import static org.terasology.rendering.opengl.DefaultDynamicFBOs.WRITE_ONLY_GBUFFER;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FBOConfig;
import static org.terasology.rendering.opengl.ScalingFactors.FULL_SCALE;
import org.terasology.rendering.opengl.fbms.DynamicFBOsManager;
import org.terasology.rendering.world.WorldRenderer;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.terasology.rendering.opengl.OpenGLUtils.bindDisplay;
import static org.terasology.rendering.opengl.OpenGLUtils.renderFullscreenQuad;
import static org.terasology.rendering.opengl.OpenGLUtils.setViewportToSizeOf;

/**
 * TODO: Add diagram of this node
 */
public class PrePostCompositeNode extends AbstractNode {
    public static final ResourceUrn REFLECTIVE_REFRACTIVE_URN = new ResourceUrn("engine:sceneReflectiveRefractive");


    @In
    private DynamicFBOsManager dynamicFBOsManager;

    @In
    private WorldRenderer worldRenderer;

    private Material prePostComposite;


    private FBO sceneReflectiveRefractive;

    @Override
    public void initialise() {
        prePostComposite = worldRenderer.getMaterial("engine:prog.combine");
        requireFBO(new FBOConfig(REFLECTIVE_REFRACTIVE_URN, FULL_SCALE, FBO.Type.HDR).useNormalBuffer(), dynamicFBOsManager);
    }

    /**
     * Adds outlines and ambient occlusion to the rendering obtained so far stored in the primary FBO.
     * Stores the resulting output back into the primary buffer.
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/prePostComposite");
        prePostComposite.enable();
        sceneReflectiveRefractive = dynamicFBOsManager.get(REFLECTIVE_REFRACTIVE_URN);

        // TODO: verify if there should be bound textures here.
        WRITE_ONLY_GBUFFER.bind();

        setViewportToSizeOf(WRITE_ONLY_GBUFFER);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // TODO: verify this is necessary

        renderFullscreenQuad();

        bindDisplay();     // TODO: verify this is necessary
        setViewportToSizeOf(READ_ONLY_GBUFFER); // TODO: verify this is necessary

        dynamicFBOsManager.swapReadWriteBuffers();

        READ_ONLY_GBUFFER.attachDepthBufferTo(sceneReflectiveRefractive);
        PerformanceMonitor.endActivity();
    }
}
