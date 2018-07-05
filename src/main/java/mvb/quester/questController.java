package mvb.quester;

import com.google.gson.Gson;
import org.powerbot.script.Condition;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Script.Manifest(name = "MVB Tutorial Island", description = "runs JSON Quests")
public class questController extends PollingScript<ClientContext> {

    JsonState[] states;
    Gson g;
    final String EXTERNALJSON = "http://pastebin.com/raw/0MpLZ82e";
    int nullCounter;
    Random r;

    @Override
    public void start() {
        nullCounter = 0;
        File f = ctx.controller.script().getStorageDirectory();
        File json = new File(f.getPath()+"/TI.json");
        String s;
        r = new Random();
        try {
            if (json.createNewFile()){
                s = populateJson(json);
            } else {
                s = new String(Files.readAllBytes(json.toPath()));
            }
            g = new Gson();
            states = g.fromJson(s, JsonState[].class);

        } catch (Exception e) {
            json.delete();
            s = ctx.controller.script().downloadString(EXTERNALJSON);
        }
    }

    private String populateJson(File json) throws FileNotFoundException {
        String s = ctx.controller.script().downloadString(EXTERNALJSON);
        PrintWriter p = new PrintWriter(json);
        p.print(s);
        p.flush();
        p.close();
        return s;
    }

    @Override
    public void poll() {
        JsonState s = state();
        if (s != null && !ctx.players.local().inMotion() && !ctx.players.local().inCombat()) {
            nullCounter = 0;
            execute(s);
        } else {
            if (s == null){
                nullCounter++;
            } else {
                nullCounter = 0;
            }
            if (nullCounter > 10){
                ctx.game.logout();
                ctx.controller.stop();
            }
            if (ctx.chat.canContinue()) {
                ctx.chat.clickContinue();
            }
            System.out.println(s);
        }
        Condition.sleep(r.nextInt(200) + 50);
    }

    public boolean execute(JsonState state) {
        Arrays.sort(state.action);
        int stage = state.action.length - 1;
        while (stage >= 0) {
            System.out.println(state.state[0].text);
            if (ctx.players.local().animation() == -1) {
                switch (state.action[stage].type) {
                    case widget:
                        widget(state.action[stage]);
                        break;
                    case npc:
                        npc(state.action[stage]);
                        break;
                    case object:
                        object(state.action[stage]);
                        break;
                    case CONTINUE:
                        contin(state.action[stage]);
                        break;
                    case useII:
                        useII(state.action[stage]);
                        break;
                    case useIO:
                        useIO(state.action[stage]);
                        break;
                    case wait:
                        Condition.sleep(r.nextInt(5000));
                        break;
                    case walk:
                        walk(state.action[stage]);
                        break;
                    case item:
                        item(state.action[stage]);
                        break;
                }
                stage--;
                Condition.sleep(r.nextInt(300) + 50);
            }
        }
        return true;
    }

    private boolean walk(JsonState.Action action) {
        List<Tile> t = new ArrayList<Tile>();
        for (int[] i : action.path){
            t.add(new Tile(i[0], i[1]).derive(r.nextInt(4) - 2, r.nextInt(4)-2));
        }
        TilePath p = ctx.movement.newTilePath(t.toArray(new Tile[t.size()]));
        while (p.valid() && ctx.movement.reachable(p.next(), ctx.players.local())){
            p.traverse();
            Condition.sleep(300);
            if (ctx.players.local().animation() == -1 && !p.traverse()){
                return true;
            }
        }

        return true;
    }

    private boolean item(JsonState.Action action) {
        prepareToUseInventory();
        Item i = ctx.inventory.select().id(Integer.valueOf(action.id)).poll();
        return i.interact(action.text);
    }

    private void prepareToUseInventory() {
        ctx.game.tab(Game.Tab.INVENTORY);
        if (ctx.inventory.selectedItemIndex() >= 0){
            ctx.inventory.selectedItem().click();
        }
    }

    private boolean useIO(JsonState.Action action) {
        prepareToUseInventory();
        Item item1 = ctx.inventory.select().id(Integer.valueOf(action.id)).poll();
        GameObject item2 = ctx.objects.select().id(Integer.valueOf(action.id2)).poll();

        if (action.bounds != null){
            item2.bounds(action.bounds);
        }
        if (item1 == null || item2 == null) {
            return false;
        }

        if (ctx.inventory.selectedItem() != item1 && !item1.interact("Use")) {
            return false;
        }
        if (!item2.inViewport()){
            ctx.camera.turnTo(item2);
        }
        return item2.interact("Use", item2.name());

    }

    private boolean useII(JsonState.Action action) {
        prepareToUseInventory();
        Item item1 = ctx.inventory.select().id(Integer.valueOf(action.id)).poll();
        Item item2 = ctx.inventory.select().id(Integer.valueOf(action.id2)).poll();

        if (item1 == null || item2 == null) {
            return false;
        }

        if (!item1.interact("Use")) {
            return false;
        }
        return item2.interact("Use");

    }

    private boolean object(JsonState.Action action) {
        GameObject o = ctx.objects.select().id(Integer.valueOf(action.id)).nearest().poll();
        if (action.bounds != null){
            o.bounds(action.bounds);
        }
        if (o == null) {
        }
        if (!o.inViewport()) {
            ctx.camera.turnTo(o);
            return false;
        }
        if (!o.interact(action.text)){
            ctx.camera.turnTo(o);
            return false;
        }else {
            Condition.sleep(r.nextInt(500) + 100);
            return true;
        }
    }

    private boolean contin(JsonState.Action action) {
        return ctx.chat.clickContinue();
    }

    private boolean npc(JsonState.Action action) {
        Condition.sleep(r.nextInt(200) + 50);
        BasicQuery<Npc> q = ctx.npcs.select().id(Integer.valueOf(action.id)).nearest();
        if (ctx.players.local().inCombat() || ctx.players.local().animation() != -1){
            return true;
        }
        Npc npc = q.poll();
        while (npc != null && npc.inCombat()) {
            npc = q.poll();
        }
        if (npc == null) {
            return false;
        }
        if (!npc.inViewport()) {
            ctx.camera.turnTo(npc);
        }
        return npc.interact(action.text);
    }

    private boolean widget(JsonState.Action action) {
        Condition.sleep(r.nextInt(200) + 50);
        Component c = getComponentFromDottedInt(action.id);
        if (!c.valid()) {
            return false;
        }
        if (!c.visible()) {
            return true;
        }

        return c.click();
    }


    public JsonState state() {
        Boolean isValid;
        Component c;
        for (JsonState js : states) {
            isValid = true;
            for (JsonState.State s : js.state) {
                if (s.type.equals("widget")) {
                    c = getComponentFromDottedInt(s.id);
                    if (!checkComponentText(c, s.text)) {
                        isValid = false;
                    }
                }
                if (s.type.equals("item")) {
                    int count = ctx.inventory.select().id(Integer.valueOf(s.id)).count();
                    if (count < s.min || count > s.max) {
                        isValid = false;
                    }
                }
            }
            if (isValid) {
                return js;
            }
        }
        return null;
    }


    public enum Actions {
        widget, npc, object, CONTINUE, useII, useIO, wait, walk, item
    }

    private class JsonState {
        State[] state;
        Action[] action;

        private class State {
            String type;
            String id;
            String text;
            int min;
            int max;
        }

        private class Action implements Comparable {
            Actions type;
            int priority;
            String id;
            String id2;
            String text;
            int[][] path;
            int[] bounds;

            @Override
            public int compareTo(Object o) {
                return this.priority - ((Action) o).priority;
            }

            @Override
            public String toString() {
                return type + ": " + id;
            }
        }

        @Override
        public String toString() {
            if (state.length > 0) {
                return state[0].text;
            }
            return "Json State Object Without any States";
        }
    }

    public Component getComponentFromDottedInt(String s) {
        String[] st = s.split("\\.");
        if (st.length < 2) {
            return null;
        }
        int[] intArr = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            intArr[i] = Integer.valueOf(st[i]);
        }
        Component c = ctx.widgets.widget(intArr[0]).component(intArr[1]);
        for (int i = 2; i < intArr.length; i++) {
            c = c.component(intArr[i]);
        }
        return c;
    }

    public boolean checkComponentText(Component c, String s) {
        return c.valid() && c.visible() && c.text().equals(s);
    }
}


