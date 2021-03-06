/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package infinity.client;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.ClientStateListener.DisconnectInfo;
import com.jme3.network.ErrorListener;
import com.jme3.network.service.ClientService;

import com.simsilica.es.EntityData;
import com.simsilica.es.client.EntityDataClientService;
import com.simsilica.ethereal.EtherealClient;
import com.simsilica.ethereal.TimeSource;
import com.simsilica.event.EventBus;
import com.simsilica.lemur.Action;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.OptionPanel;
import com.simsilica.lemur.OptionPanelState;
import com.simsilica.state.CompositeAppState;

//import com.simsilica.demo.net.AccountSessionListener;
//import com.simsilica.demo.client.AccountClientService;
////import com.simsilica.demo.view.GameSessionState;
//import com.simsilica.demo.view.LobbyState;

/**
 * Manages the connection and game client when connected to a server.
 *
 * @author Paul Speed
 */
public class ConnectionState extends CompositeAppState {

    static Logger log = LoggerFactory.getLogger(ConnectionState.class);

    private final AppState parent;

    private final String host;
    private final int port;
    private final boolean autoLogin;

    private GameClient client;
    private final ConnectionObserver connectionObserver = new ConnectionObserver();
    private Connector connector;
    private Thread renderThread;

    private OptionPanel connectingPanel;

    private volatile boolean closing;

    public ConnectionState(final AppState parent, final String host, final int port) {
        this(parent, host, port, false);
    }

    public ConnectionState(final AppState parent, final String host, final int port, final boolean autoLogin) {
        this.parent = parent;
        this.host = host;
        this.port = port;
        this.autoLogin = autoLogin;
    }

    public int getClientId() {
        return client.getClient().getId();
    }

    public TimeSource getRemoteTimeSource() {
        return getService(EtherealClient.class).getTimeSource();
    }

    public EntityData getEntityData() {
        return getService(EntityDataClientService.class).getEntityData();
    }

    public <T extends ClientService> T getService(final Class<T> type) {
        if (client == null) {
            return null;
        }
        return client.getService(type);
    }

    public void disconnect() {
        log.info("disconnect()");
        closing = true;
        log.info("Detaching ConnectionState");
        getStateManager().detach(this);
        log.info("Detached ConnectionState");
    }

    public boolean join(final String userName) {
        String n = userName;
        log.info("join(" + n + ")");

        if (n != null) {
            n = n.trim();
        }

        if (Strings.isNullOrEmpty(n)) {
            showError("Join Error", "Please specify a player name for use in game.", null, false);
            return false;
        }

        // So here we'd login and then when we get a response from the
        // server that we are logged in then we'd launch the game state and
        // so on... for now we'll just do it directly.
        // client.getService(AccountClientService.class).login(userName);

        return true;
    }

    protected void onLoggedOn(final boolean loggedIn) {
        if (!loggedIn) {
            // We'd want to present an error... but right now this will
            // never happen.
        }
        // addChild(new GameSessionState(), true);
        // addChild(new LobbyState(), true);
    }

    @Override
    protected void initialize(final Application app) {

        // Just double checking we aren't double-connecting because of some
        // bug
        if (getState(ConnectionState.class) != this) {
            throw new RuntimeException("More than one ConnectionState is not yet allowed.");
        }

        connectingPanel = new OptionPanel("Connecting...", new ExitAction("Cancel", true));
        getState(OptionPanelState.class).show(connectingPanel);

        renderThread = Thread.currentThread();
        connector = new Connector();
        connector.start();
    }

    @Override
    protected void cleanup(final Application app) {
        closing = true;
        if (client != null) {
            client.close();
        }

        // Close the connecting panel if it's still open
        closeConnectingPanel();

        // And re-enable the parent
        parent.setEnabled(true);
    }

    protected void closeConnectingPanel() {
        if (getState(OptionPanelState.class).getCurrent() == connectingPanel) {
            getState(OptionPanelState.class).close();
        }
    }

    @Override
    protected void onEnable() {
        return;
    }

    @Override
    protected void onDisable() {
        return;
    }

    protected boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    protected void showError(final String title, final Throwable e, final boolean fatal) {
        showError(title, null, e, fatal);
    }

    protected void showError(final String title, final String message, final Throwable e, final boolean fatal) {
        if (isRenderThread()) {
            String m = message;
            if (e != null) {
                if (m != null) {
                    m += "\n";
                } else {
                    m = "";
                }
                m += e.getClass().getSimpleName() + ":" + e.getMessage();
            }
            getState(OptionPanelState.class).show(title, m, new ExitAction(fatal));
        } else {
            getApplication().enqueue(new Callable<>() {
                @Override
                public Object call() {
                    showError(title, e, fatal);
                    return null;
                }
            });
        }
    }

    protected void setClient(final GameClient client) {
        log.info("Connection established:" + client);
        if (isRenderThread()) {
            this.client = client;
        } else {
            getApplication().enqueue(new Callable<>() {
                @Override
                public Object call() {
                    setClient(client);
                    return null;
                }
            });
        }
    }

    protected void onConnected() {
        log.info("onConnected()");

        EventBus.publish(ClientEvent.clientConnected, new ClientEvent());

        closeConnectingPanel();

        // Add our client listeners
        // client.getService(AccountClientService.class).addAccountSessionListener(new
        // AccountObserver());

        // String serverInfo =
        // client.getService(AccountClientService.class).getServerInfo();

        // log.debug("Server info:" + serverInfo);

        if (autoLogin) {
            join(System.getProperty("user.name"));
        } else {
            // getStateManager().attach(new LoginState(serverInfo));
        }
    }

    protected void onDisconnected(final DisconnectInfo info) {
        log.info("onDisconnected(" + info + ")");
        EventBus.publish(ClientEvent.clientDisconnected, new ClientEvent());
        closeConnectingPanel();
        if (!closing) {
            if (info != null) {
                showError("Disconnect", info.reason, info.error, true);
            } else {
                showError("Disconnected", "Unknown error", null, true);
            }
        }
        log.info("Detaching ConnectionState");
        getStateManager().detach(this);
        log.info("Detached ConnectionState");
    }

    private class ExitAction extends Action {
        private final boolean close;

        public ExitAction(final boolean close) {
            this("Ok", close);
        }

        public ExitAction(final String name, final boolean close) {
            super(name);
            this.close = close;
        }

        @Override
        public void execute(final Button source) {
            if (close) {
                disconnect();
            }
        }
    }

    private class ConnectionObserver implements ClientStateListener, ErrorListener<Client> {
        @Override
        public void clientConnected(final Client c) {
            log.info("clientConnected(" + c + ")");
            getApplication().enqueue(new Callable<>() {
                @Override
                public Object call() {
                    onConnected();
                    return null;
                }
            });
        }

        @Override
        public void clientDisconnected(final Client c, final DisconnectInfo info) {
            log.info("clientDisconnected(" + c + ", " + info + ")");
            getApplication().enqueue(new Callable<>() {
                @Override
                public Object call() {
                    onDisconnected(info);
                    return null;
                }
            });
        }

        @Override
        public void handleError(final Client source, final Throwable t) {
            log.error("Connection error", t);
            showError("Connection Error", t, true);
        }
    }

    // private class AccountObserver implements AccountSessionListener {
    //
    // public void notifyLoginStatus( final boolean loggedIn ) {
    // getApplication().enqueue(new Callable<Object>() {
    // public Object call() {
    // onLoggedOn(loggedIn);
    // return null;
    // }
    // });
    // }
    // }

    private class Connector extends Thread {

        public Connector() {
            super();
        }

        @Override
        public void run() {

            try {
                log.info("Creating game client for:" + host + " " + port);
                final GameClient c = new GameClient(host, port);
                if (closing) {
                    return;
                }
                setClient(c);
                c.getClient().addClientStateListener(connectionObserver);
                c.getClient().addErrorListener(connectionObserver);
                if (closing) {
                    return;
                }

                log.info("Starting client...");
                c.start();
                log.info("Client started.");
            } catch (final IOException e) {
                if (closing) {
                    return;
                }
                showError("Error Connecting", e, true);
            }
        }
    }
}
