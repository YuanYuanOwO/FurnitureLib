package de.Ste3et_C0st.FurnitureLib.main.Protection;

import de.Ste3et_C0st.FurnitureLib.Events.FurnitureRegionClear;
import de.Ste3et_C0st.FurnitureLib.ModelLoader.Block.ModelBlockAquaticUpdate;
import de.Ste3et_C0st.FurnitureLib.Utilitis.LanguageManager;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureConfig;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.EventType;
import de.Ste3et_C0st.FurnitureLib.main.Type.PlaceableSide;
import de.Ste3et_C0st.FurnitureLib.main.Type.PublicMode;
import de.Ste3et_C0st.ProtectionLib.main.ProtectionLib;
import de.Ste3et_C0st.ProtectionLib.main.protectionObj;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProtectionManager {

    public ProtectionManager(Plugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtectionLib")) {
            new FurnitureRegionClear();
        }
    }

    public boolean isSolid(Material mat, PlaceableSide side, Block block) {
        if (!checkPlaceable(mat, side, block))  return false;
        return mat.isSolid();
    }

    private boolean checkPlaceable(Material mat, PlaceableSide side, Block block) {
        return mat.isBlock() && mat.isSolid() && (mat.isOccluding() || blockStateParser(block));
    }

    private boolean blockStateParser(Block block) {
		return !FurnitureLib.isNewVersion() || ModelBlockAquaticUpdate.isSolid(block);
    }

    public int getSize() {
        if (isEnabled()) {
            return 0;
        }
        return ProtectionLib.getInstance().getWatchers().size();
    }
    
    public boolean isEnabled() {
    	 return Bukkit.getPluginManager().isPluginEnabled("ProtectionLib");
    }

    public boolean useProtectionLib() {
    	return isEnabled();
    }

    public boolean canBuild(Player p, Location loc) {
        if (isEnabled() == false) {
            return true;
        }
        return ProtectionLib.getInstance().canBuild(loc, p);
    }

    public Boolean isOwner(Player p, Location loc) {
        if (isEnabled() == false) {
            return true;
        }
        return ProtectionLib.getInstance().isOwner(loc, p);
    }
    
    public List<JsonObject> getProtectionClazz() {
    	if (isEnabled() == false) {
            return null;
        }
    	List<JsonObject> json = new ArrayList<JsonObject>();
    	for(protectionObj protection : ProtectionLib.getInstance().getWatchers()) {
    		JsonObject object = new JsonObject();
    		object.addProperty("plugin", protection.getPlugin().getDescription().getName());
    		object.addProperty("clazz", protection.getClass().getSimpleName());
    		json.add(object);
    	}
    	return json;
    }

    public boolean canBuild(Player p, ObjectID id, EventType type) {
        return canBuild(p, id, type, true);
    }

    public boolean canBuild(Player p, ObjectID id, EventType type, boolean sendMessage) {
        if (p.isOp() || FurnitureLib.getInstance().getPermission().hasPerm(p, "furniture.bypass.protection") || FurnitureLib.getInstance().getPermission().hasPerm(p, "furniture.admin")) {
            return true;
        }
        PublicMode publicMode = id.getPublicMode();
        UUID userID = p.getUniqueId();
        UUID ownerID = id.getUUID();
        EventType eventType = id.getEventType();
        if (ownerID == null) {
            return true;
        }
        
        if(!isProtectedRegion(id)) {
        	if(id.getUUID().equals(userID)) return true;
        	if(EventType.INTERACT == type) {
        		if(EventType.INTERACT == eventType || EventType.BREAK_INTERACT == eventType) {
        			return true;
        		}
        	}else if(EventType.BREAK == type) {
        		if(id.getMemberList().contains(userID)) {
            		if(EventType.BREAK == id.getEventType() || EventType.BREAK_INTERACT == id.getEventType()) {
            			return true;
            		}
            	}
        	}
        	if(sendMessage) {
        		 p.sendMessage(LanguageManager.getInstance().getString("message.NoPermissions"));
        	}
        	return false;
        }
        
        boolean b = canBuild(p, id);
        if (b) return true;
        if (publicMode.equals(PublicMode.PRIVATE)) {
            b = ownerID.equals(userID);
        } else if (publicMode.equals(PublicMode.MEMBERS)) {
            if (id.getMemberList().isEmpty()) {
                b = false;
            }
            if (id.getMemberList().contains(userID)) {
                b = isEventType(id, type);
            }
        } else if (publicMode.equals(PublicMode.PUBLIC)) {
            b = isEventType(id, type);
        }
        if (!b && sendMessage) {
            p.sendMessage(LanguageManager.getInstance().getString("message.NoPermissions"));
        }
        return b;
    }

    private boolean canBuild(Player p, ObjectID id) {
        if (p.getUniqueId().equals(id.getUUID())) return true;
        if (isEnabled() == false) return true;
        if (getSize() == 0) return true;
        boolean memberOfRegion = canBuild(p, id.getStartLocation());
        boolean ownerOfRegion = isOwner(p, id.getStartLocation());
        
        if (memberOfRegion && !ownerOfRegion) {
			return p.getUniqueId().equals(id.getUUID());
		}
        
        if (ownerOfRegion) {
            if (!p.getUniqueId().equals(id.getUUID())) {
                return true;
            }
        }

        if (memberOfRegion && ownerOfRegion && FurnitureConfig.getFurnitureConfig().haveRegionMemberAccess()) {
            if (!p.getUniqueId().equals(id.getUUID())) {
                return true;
            }
            return true;
        }
        return false;
    }
    
    private boolean isProtectedRegion(ObjectID objectID) {
    	if (isEnabled() == false) return false;
    	return ProtectionLib.getInstance().isProtectedRegion(objectID.getStartLocation());
    }

    private boolean isEventType(ObjectID objID, EventType type) {
		return objID.getEventType().equals(type) || objID.getEventType().equals(EventType.BREAK_INTERACT);
	}
}
