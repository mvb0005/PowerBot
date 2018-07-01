package mvb.quester;

import com.google.gson.Gson;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;

import java.util.Map;


@Script.Manifest(name = "MVB Quester", description = "runs JSON Quests")
public class questController extends PollingScript<ClientContext>{

    JsonState[] states;
    @Override
    public void start() {

        try {
            String s = ctx.controller.script().downloadString("http://pastebin.com/raw/TsM1EZzp");
            Gson g = new Gson();
            states = g.fromJson(s, JsonState[].class);
            for (JsonState js : states){
                Component c = getComponentFromDottedInt((String) js.state.keySet().toArray()[0]);
                s = (String) js.state.values().toArray()[0];
                System.out.println(checkComponentText(c,s));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void poll() {
        ctx.controller.stop();
    }

    public State state(){
        return null;
    }

    private enum State{
        WIDGET, NPC, CONTINUE, OBJECT, USE
    }

    private class JsonState{
        Map<String, String> state;
        Map<String, Object> action;
    }

    private class Actions{

    }

    public Component getComponentFromDottedInt(String s){
        String[] st = s.split("\\.");
        if (st.length < 2){
            return null;
        }
        int[] intArr = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            intArr[i] = Integer.valueOf(st[i]);
        }
        Component c = ctx.widgets.widget(intArr[0]).component(intArr[1]);
        for (int i = 2; i < intArr.length; i++){
            c = c.component(intArr[i]);
        }
        return c;
    }

    public boolean checkComponentText(Component c, String s){
        System.out.println(c.text() + ", " + s);
        return c.visible() && c.text() == s;
    }
}


