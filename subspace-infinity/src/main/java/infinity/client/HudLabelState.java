/* 
 * Copyright (c) 2018, Asser Fahrenholz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package infinity.client;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;

import com.simsilica.lemur.*;
import com.simsilica.lemur.style.ElementId;

import com.simsilica.ethereal.TimeSource;

import com.simsilica.es.*;

import com.simsilica.mathd.trans.PositionTransition3f;
import com.simsilica.mathd.trans.TransitionBuffer;

import infinity.ConnectionState;
import infinity.Main;
import infinity.TimeState;
import infinity.api.es.BodyPosition;

/**
 * Displays a HUD label for any entity with a BodyPosition and a Name.
 *
 * @author Paul Speed
 */
public class HudLabelState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(HudLabelState.class);

    private EntityData ed;
    private TimeSource timeSource;
    private TimeState timeState;

    private Node hudLabelRoot;
    private Camera camera;

    private LabelContainer labels;
    private EntityId playerEntityId;
    private EntityId shipEntityId;

    public HudLabelState() {

        log.debug("Constructed HudLabelState");
    }

    @Override
    protected void initialize(Application app) {
        hudLabelRoot = new Node("HUD labels");

        this.camera = app.getCamera();

        // Retrieve the time source from the network connection
        // The time source will give us a time in recent history that we should be
        // viewing.  This currently defaults to -100 ms but could vary (someday) depending
        // on network connectivity.
        // For more information on this interpolation approach, see the Valve networking
        // articles at:
        // https://developer.valvesoftware.com/wiki/Source_Multiplayer_Networking
        // https://developer.valvesoftware.com/wiki/Latency_Compensating_Methods_in_Client/Server_In-game_Protocol_Design_and_Optimization
        //this.timeSource = getState(ConnectionState.class).getRemoteTimeSource();
        this.timeState = getState(TimeState.class);

        this.ed = getState(ConnectionState.class).getEntityData();
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {

        labels = new LabelContainer(ed);
        labels.start();

        ((Main) getApplication()).getGuiNode().attachChild(hudLabelRoot);
        //this.timeSource = getState(TimeState.class).getTimeSource();
    }

    @Override
    protected void onDisable() {
        hudLabelRoot.removeFromParent();

        labels.stop();
        labels = null;
    }

    @Override
    public void update(float tpf) {

        // Grab a consistent time for this frame
        long time = timeState.getTime();
        // Grab a consistent time for this frame
        //long time = timeSource.getTime();

        if (playerEntityId != null) {
            // Update all of the models
            labels.update();
            for (LabelHolder label : labels.getArray()) {
                label.update(time);
            }
        }
    }

    /**
     * Holds the on-screen label and the transition buffer, etc necessary for
     * managing the position and state of the label. If not for the need to poll
     * these once per frame for position updates, we technically could have done
     * all management in the EntityContainer and just returned Labels directly.
     */
    private class LabelHolder {

        Entity entity;
        Label label;
        float labelOffset = 0.1f;

        boolean visible;
        boolean isPlayerEntity;

        TransitionBuffer<PositionTransition3f> buffer;

        public LabelHolder(Entity entity) {
            this.entity = entity;

            this.label = new Label("Ship", new ElementId("ship.label"));
            label.setColor(ColorRGBA.Green);
            label.setShadowColor(ColorRGBA.Black);

            BodyPosition bodyPos = entity.get(BodyPosition.class);
            // BodyPosition requires special management to make
            // sure all instances of BodyPosition are sharing the same
            // thread-safe history buffer.  Everywhere it's used, it should
            // be 'initialized'.            
            bodyPos.initialize(entity.getId(), 12);
            buffer = bodyPos.getBuffer();

            // If this is the player's ship then we don't want the model
            // shown else it looks bad.  A) it's ugly.  B) the model will
            // always lag the player's turning.
            if (shipEntityId != null && entity.getId().getId() == shipEntityId.getId()) {
                this.isPlayerEntity = true;
            }

            // Pick up the current name
            updateComponents();
        }

        protected void updateLabelPos(Vector3f pos) {
            if (!visible || (shipEntityId != null && isPlayerEntity)) {
                return;
            }
            Vector3f camRelative = pos.subtract(camera.getLocation());
            float distance = camera.getDirection().dot(camRelative);
            if (distance < 0) {
                // It's behind us
                label.removeFromParent();
                return;
            }

            // Calculate the ship's position on screen
            Vector3f screen2 = camera.getScreenCoordinates(pos.add(0, labelOffset, 0));

            Vector3f pref = label.getPreferredSize();
            label.setLocalTranslation(screen2.x - pref.x * 0.5f, screen2.y + pref.y, screen2.z);
            if (label.getParent() == null) {
                hudLabelRoot.attachChild(label);
            }
        }

        public void update(long time) {

            // Look back in the brief history that we've kept and
            // pull an interpolated value.  To do this, we grab the
            // span of time that contains the time we want.  PositionTransition
            // represents a starting and an ending pos+rot over a span of time.
            PositionTransition3f trans = buffer.getTransition(time);
            if (trans != null) {
                Vector3f pos = trans.getPosition(time, true);
                setVisible(trans.getVisibility(time));
                updateLabelPos(pos);
            }
        }

        protected void updateComponents() {
            label.setText(entity.get(Name.class).getName());
        }

        protected void setVisible(boolean f) {
            if (this.visible == f) {
                return;
            }
            this.visible = f;
            if (visible && !isPlayerEntity) {
                label.setCullHint(Spatial.CullHint.Inherit);
            } else {
                label.setCullHint(Spatial.CullHint.Always);
            }
        }

        public void dispose() {
            label.removeFromParent();
        }
    }

    private class LabelContainer extends EntityContainer<LabelHolder> {

        public LabelContainer(EntityData ed) {
            super(ed, Name.class, BodyPosition.class);
        }

        @Override
        protected LabelHolder[] getArray() {
            return super.getArray();
        }

        @Override
        protected LabelHolder addObject(Entity e) {
            return new LabelHolder(e);
        }

        @Override
        protected void updateObject(LabelHolder object, Entity e) {
            object.updateComponents();
        }

        @Override
        protected void removeObject(LabelHolder object, Entity e) {
            object.dispose();
        }
    }

    public void setPlayerEntityIds(EntityId playerEntityId, EntityId shipEntityId) {
        this.playerEntityId = playerEntityId;
        this.shipEntityId = shipEntityId;
    }

}
