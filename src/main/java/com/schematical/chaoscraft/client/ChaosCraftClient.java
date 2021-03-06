package com.schematical.chaoscraft.client;

import com.schematical.chaoscraft.ChaosCraft;
import com.schematical.chaoscraft.client.gui.*;
import com.schematical.chaoscraft.entities.OrgEntity;
import com.schematical.chaoscraft.network.ChaosNetworkManager;
import com.schematical.chaoscraft.network.packets.*;
import com.schematical.chaosnet.model.ChaosNetException;
import com.schematical.chaosnet.model.PostUsernameTrainingroomsTrainingroomSessionsSessionRepairRequest;
import com.schematical.chaosnet.model.PostUsernameTrainingroomsTrainingroomSessionsSessionRepairResult;
import com.schematical.chaosnet.model.TrainingRoomSessionNextResponse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ChaosCraftClient {

    public TrainingRoomSessionNextResponse lastResponse;
    private int ticksSinceLastSpawn;

    protected State state = State.Uninitiated;
    protected String trainingRoomNamespace;
    protected String trainingRoomUsernameNamespace;
    protected String sessionNamespace;

    public ArrayList<String> _debugSpawnedOrgNamespaces = new ArrayList<String>();
    public ArrayList<String> _debugReportedOrgNamespaces = new ArrayList<String>();
    public int consecutiveErrorCount = 0;
    public HashMap<String, ClientOrgManager> newOrganisms = new HashMap<String, ClientOrgManager>();
    public HashMap<String, ClientOrgManager> myOrganisms = new HashMap<String, ClientOrgManager>();
    public Thread thread;
    public static List<KeyBinding> keyBindings = new ArrayList<KeyBinding>();
    private int ticksRequiredToCallChaosNet = 100;


    public void onWorldUnload() {
        state = State.Uninitiated;
        myOrganisms.clear();

    }


    public void setTrainingRoomInfo(ServerIntroInfoPacket serverInfo) {
        trainingRoomNamespace = serverInfo.getTrainingRoomNamespace();
        trainingRoomUsernameNamespace = serverInfo.getTrainingRoomUsernameNamespace();
        sessionNamespace = serverInfo.getSessionNamespace();
        ChaosCraft.LOGGER.info("TrainingRoomInfo Set: " + trainingRoomNamespace + ", " + trainingRoomUsernameNamespace + ", " + sessionNamespace);
        state = State.Authed;
        if((Minecraft.getInstance().currentScreen instanceof ChaosTrainingRoomSelectionOverlayGui)) {

            Minecraft.getInstance().displayGuiScreen((Screen)null);
        }
    }

    public State getState() {
        return state;
    }
    public String getTrainingRoomNamespace(){
        return trainingRoomNamespace;
    }
    public String getTrainingRoomUsernameNamespace(){
        return trainingRoomUsernameNamespace;
    }
    public String getSessionNamespace(){
        return sessionNamespace;
    }

    public void preInit(){
        keyBindings.add(new KeyBinding(CCKeyBinding.SHOW_ORG_LIST,79, "key.chaoscraft"));
        keyBindings.add(new KeyBinding(CCKeyBinding.OBSERVER_MODE, 0x18, "key.chaoscraft"));
        keyBindings.add(new KeyBinding(CCKeyBinding.SHOW_SPECIES_LIST, 0x24, "key.chaoscraft"));
 /*       keyBindings.add(new KeyBinding(CCKeyBinding.UP, 87, "key.chaoscraft"));
        keyBindings.add(new KeyBinding(CCKeyBinding.LEFT, 65, "key.chaoscraft"));
        keyBindings.add(new KeyBinding(CCKeyBinding.DOWN, 83, "key.chaoscraft"));
        keyBindings.add(new KeyBinding(CCKeyBinding.RIGHT, 70, "key.chaoscraft"));*/

// register all the key bindings
        for (int i = 0; i < keyBindings.size(); ++i)
        {
            ClientRegistry.registerKeyBinding(keyBindings.get(i));
        }
        if(ChaosCraft.config.username == null){
            if(!(Minecraft.getInstance().currentScreen instanceof ChaosAuthOverlayGui)) {
                ChaosAuthOverlayGui screen = new ChaosAuthOverlayGui();
                Minecraft.getInstance().displayGuiScreen(screen);
            }
            return;
        }
    }
    public void setTicksRequiredToCallChaosNet(int i){
        ticksRequiredToCallChaosNet = i;
    }
    public void init(){
        if(Minecraft.getInstance().getConnection() == null){
            return;
        }

        if(ChaosCraft.config.accessToken == null){
            //MAKE THEM AUTH FIRST
            //but only open the screen when it isnt already open
            if(!(Minecraft.getInstance().currentScreen instanceof ChaosAuthOverlayGui)) {
                ChaosAuthOverlayGui screen = new ChaosAuthOverlayGui();
                Minecraft.getInstance().displayGuiScreen(screen);
            }
            return;
        }


        ChaosCraft.LOGGER.info("Client Sending Auth!!");
        //!!!!!!!
        ChaosNetworkManager.sendToServer(new ClientAuthPacket(ChaosCraft.config.accessToken));
        state = State.AuthSent;
        //Get info on what the server is running


    }
    public void attachOrgToEntity(String orgNamespace, int entityId) {
        OrgEntity orgEntity = (OrgEntity)Minecraft.getInstance().world.getEntityByID(entityId);
        if(orgEntity == null){
            ChaosCraft.LOGGER.error("Client could not find entityId: " + entityId + " to attach org: " + orgNamespace);

            Iterator<Entity> iterator = Minecraft.getInstance().world.getAllEntities().iterator();
            while(iterator.hasNext() && orgEntity == null){
                Entity entity = iterator.next();
                if( entity.getDisplayName().getString().equals(orgNamespace)) {
                    ChaosCraft.LOGGER.error("Client found a potential match after all entityId: " + entity.getEntityId() + " will attach org: " + orgNamespace + " == " + entity.getDisplayName().getString());
                    orgEntity = (OrgEntity) entity;
                }
                /*if(entity instanceof  OrgEntity){
                    OrgEntity testOrgEntity = (OrgEntity) entity;
                    if(testOrgEntity.)

                }*/


            }
            if(orgEntity == null) {//If it is still null lets drop out
                //orgsQueuedToSpawn.remove(orgNamespace);
                ChaosCraft.LOGGER.error("TODO: Requeue for spawn");// Try the spawn over again. If it fails again then the server will just let us know again which one it is
                return;
            }
        }
        ClientOrgManager clientOrgManager = myOrganisms.get(orgNamespace);
        clientOrgManager.attachOrgEntity(orgEntity);

    }
    public List<ClientOrgManager> getOrgsWithState(ClientOrgManager.State state){
        List<ClientOrgManager> orgManagers = new ArrayList<ClientOrgManager>();
        for (ClientOrgManager clientOrgManager : myOrganisms.values()) {
            if(clientOrgManager.getState().equals(state)){
                orgManagers.add(clientOrgManager);
            }
        }
        return orgManagers;
    }
    public void tick(){

        if(!state.equals(State.Authed)){
            return;
        }
        /*if(true){
            return;
        }*/
        checkForDeadOrgs();
        startSpawnOrgs();
        int liveOrgCount = getLiveOrgCount();
        ticksSinceLastSpawn += 1;



        //List<ClientOrgManager> deadOrgs = getDeadOrgs();
        List<ClientOrgManager> orgsReadyToReport = getOrgsWithState(ClientOrgManager.State.ReadyToReport);
        if (
            orgsReadyToReport.size() > 0 ||
            (
                ticksSinceLastSpawn > (ticksRequiredToCallChaosNet) &&
                (liveOrgCount) < ChaosCraft.config.maxBotCount
            )
        ) {

            if(thread == null) {
                if(newOrganisms.size() > 0){
                    cleanUp();
                    Iterator<String> iterator = newOrganisms.keySet().iterator();
                   while(iterator.hasNext()){
                        String namespace = iterator.next();
                        myOrganisms.put(namespace, newOrganisms.get(namespace));
                        iterator.remove();
                    }
                }
                ticksSinceLastSpawn = 0;
                ticksRequiredToCallChaosNet = 100;
                thread = new Thread(new ChaosClientThread(), "ChaosClientThread");
                thread.start();
            }
        }

        if(consecutiveErrorCount > 5){
            throw new ChaosNetException("ChaosCraft.consecutiveErrorCount > 5");
        }



    }

    public void updateObservers(){
        /*if(myOrganisims.size() > 0) {
            for (EntityPlayerMP observingPlayer : observingPlayers) {
                Entity entity = observingPlayer.getSpectatingEntity();

                if (
                        entity.equals(observingPlayer) ||
                        entity == null ||
                        entity.isDead
                ) {
                    if(
                            entity != null &&
                                    entity instanceof OrgEntity
                    ){
                        ((OrgEntity) entity).setObserving(null);
                    }
                    int index = (int) Math.floor(myOrganisims.size() * Math.random());
                    OrgEntity orgToObserve = myOrganisims.get(index);
                    orgToObserve.setObserving(observingPlayer);
                    observingPlayer.setSpectatingEntity(orgToObserve);
                }
            }
        }*/
    }
    public List<ClientOrgManager> checkForDeadOrgs(){

        List<ClientOrgManager> clientOrgManagers = getOrgsWithState(ClientOrgManager.State.Ticking);
        for (ClientOrgManager clientOrgManager : clientOrgManagers) {

            if (!clientOrgManager.getEntity().isAlive()) {

                clientOrgManager.markDead();

            }
        }
        return clientOrgManagers;
    }
    private int getLiveOrgCount() {

        Iterator<ClientOrgManager> iterator = myOrganisms.values().iterator();
        int liveOrgCount = 0;
        while (iterator.hasNext()) {
            ClientOrgManager clientOrgManager = iterator.next();
            clientOrgManager.manualUpdateCheck();
            if (
                clientOrgManager.getOrganism() == null// ||
                //organism.getSpawnHash() != ChaosCraft.spawnHash
            ) {
                clientOrgManager.getEntity().setHealth(-1);
                iterator.remove();
                //ChaosCraft.logger.info("Setting Dead: " + organism.getName() + " - Has no `Organism` record");
            }
            if (
                clientOrgManager.getEntity() != null &&
                clientOrgManager.getEntity().isAlive()
            ) {
                liveOrgCount += 1;
            }
        }
        return liveOrgCount;

    }

    private void startSpawnOrgs() {

        Iterator<ClientOrgManager> iterator = getOrgsWithState(ClientOrgManager.State.OrgAttached).iterator();

        while (iterator.hasNext()) {
            ClientOrgManager clientOrgManager = iterator.next();

            if (_debugSpawnedOrgNamespaces.contains(clientOrgManager.getCCNamespace())) {
                ChaosCraft.LOGGER.error("Client already tried to spawn: " + clientOrgManager.getCCNamespace()  + " State: " + clientOrgManager.getState());
            } else {
                _debugSpawnedOrgNamespaces.add(clientOrgManager.getCCNamespace());
            }
            CCClientSpawnPacket packet = new CCClientSpawnPacket(
                clientOrgManager.getCCNamespace()
            );
            clientOrgManager.markSpawnMessageSent();

            ChaosNetworkManager.sendToServer(packet);

        }
    }



    @SubscribeEvent
    public  void onKeyInputEvent(InputEvent.KeyInputEvent event) {
        //ChaosCraft.LOGGER.info("KeyDonw: " + event.getKey());
        for (KeyBinding keyBinding : keyBindings) {
            // check each enumerated key binding type for pressed and take appropriate action
            if (keyBinding.isPressed()) {
                // DEBUG
                switch(keyBinding.getKeyDescription()){
                    case(CCKeyBinding.SHOW_ORG_LIST):
                        //CCOrgListView view = new CCOrgListView();

                        ChaosInGameMenuOverlayGui screen = new ChaosInGameMenuOverlayGui();
                        Minecraft.getInstance().displayGuiScreen(screen);
                        break;
                    case(CCKeyBinding.SHOW_SPECIES_LIST):
                       /* CCSpeciesListView view2 = new CCSpeciesListView();

                        Minecraft.getInstance().displayGuiScreen(view2);*/
                        break;
                    case(CCKeyBinding.OBSERVER_MODE):
                      /*  List<EntityPlayerMP> players = Minecraft.getMinecraft().world.<EntityPlayerMP>getPlayers(EntityPlayerMP.class, new Predicate<EntityPlayerMP>() {
                            @Override
                            public boolean apply(@Nullable EntityPlayerMP input) {
                                return true;
                            }
                        });
                        for(EntityPlayerMP player: players){
                            ChaosCraft.client.toggleObservingPlayer(player);
                        }*/
                        break;
                    case(CCKeyBinding.UP):
                    case(CCKeyBinding.DOWN):
                    case(CCKeyBinding.LEFT):
                    case(CCKeyBinding.RIGHT):

                        break;
                }

                // do stuff for this key binding here
                // remember you may need to send packet to server


            }
        }


    }

    public void attachScoreEventToEntity(CCServerScoreEventPacket message) {
        if(!myOrganisms.containsKey(message.orgNamespace)){
            ChaosCraft.LOGGER.error("attatchScoreEventToEntity - Cannot find orgNamespace: " + message.orgNamespace);
            return;
        }
        myOrganisms.get(message.orgNamespace).addServerScoreEvent(message);
    }

    public int getTicksSinceLastSpawn() {
        return ticksSinceLastSpawn;
    }
    public HashMap<ClientOrgManager.State, ArrayList<ClientOrgManager>> getOrgsSortedByState(){
        HashMap<ClientOrgManager.State, ArrayList<ClientOrgManager>> coll = new HashMap<ClientOrgManager.State, ArrayList<ClientOrgManager>>();
        for (ClientOrgManager clientOrgManager : myOrganisms.values()) {
            if(!coll.containsKey(clientOrgManager.getState())){
                coll.put(clientOrgManager.getState(), new ArrayList<ClientOrgManager>());
            }
            coll.get(clientOrgManager.getState()).add(clientOrgManager);
        }
        return coll;
    }

    public void showOrdDetailOverlay(ClientOrgManager clientOrgManager) {
        ChaosOrgDetailOverlayGui screen = new ChaosOrgDetailOverlayGui(clientOrgManager);
        Minecraft.getInstance().displayGuiScreen(screen);
    }
    public void cleanUp(){
        Iterator<ClientOrgManager> iterator = myOrganisms.values().iterator();
        while(iterator.hasNext()){

            ClientOrgManager clientOrgManager = iterator.next();
            if(clientOrgManager.getState().equals(ClientOrgManager.State.FinishedReport)){
                iterator.remove();
            }
        }
    }
    public void repair(){
        try{

            PostUsernameTrainingroomsTrainingroomSessionsSessionRepairRequest request = new PostUsernameTrainingroomsTrainingroomSessionsSessionRepairRequest();
            request.setUsername(trainingRoomUsernameNamespace);
            request.setTrainingroom(trainingRoomNamespace);
            request.setSession(sessionNamespace);
            PostUsernameTrainingroomsTrainingroomSessionsSessionRepairResult response = ChaosCraft.sdk.postUsernameTrainingroomsTrainingroomSessionsSessionRepair(request);

        }catch(ChaosNetException exception){
            ByteBuffer byteBuffer = exception.sdkHttpMetadata().responseContent();
            String message = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            exception.setMessage(message);
            throw exception;
        }
    }

    public void displayTrainingRoomSelectionOverlayGui() {

        ChaosTrainingRoomSelectionOverlayGui screen = new ChaosTrainingRoomSelectionOverlayGui();
        Minecraft.getInstance().displayGuiScreen(screen);
    }

    public enum State{
        Uninitiated,
        AuthSent,
        Authed
    }
}
