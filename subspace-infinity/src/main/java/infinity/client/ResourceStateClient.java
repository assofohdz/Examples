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

import com.jme3.app.Application;
import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.state.DebugHudState;

/**
 *
 * @author Asser
 */
public class ResourceStateClient extends BaseAppState {

    private VersionedHolder<String> goldDisplay;

    @Override
    protected void initialize(Application aplctn) {
        if (getState(DebugHudState.class) != null) {
            DebugHudState debug = getState(DebugHudState.class);
            this.goldDisplay = debug.createDebugValue("Gold", DebugHudState.Location.Top);
        }
    }

    @Override
    protected void cleanup(Application aplctn) {
    }

    @Override
    protected void enable() {

    }

    @Override
    protected void disable() {

    }
    
    public void updateCredits(int credits){
        goldDisplay.setObject(String.valueOf(credits));
    }

    @Override
    public void update(float tpf) {
        
    }
}