package ru.l2gw.gameserver.serverpackets;

//import ru.l2gw.gameserver.tables.SkillTable;
//import ru.l2gw.gameserver.model.L2Character;
//import ru.l2gw.gameserver.model.L2Effect;
//import ru.l2gw.gameserver.model.L2Player;
//import ru.l2gw.gameserver.model.IconEffect;
//import ru.l2gw.gameserver.model.L2Skill;
//import ru.l2gw.util.EffectsComparator;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;

/** 
 * @author    ALF 
 * @data    07.02.2012 
 */ 
//public class ExAbnormalStatusUpdateFromTarget extends L2GameServerPacket implements IconEffectPacket 
//{ 
    //private int objId; 
    //private List<IconEffect> _effects; 
    // 
    //public ExAbnormalStatusUpdateFromTarget(L2Player target)  
    //{ 
    //    _effects = new ArrayList<IconEffect>();         
    //    objId = target.getObjectId();     
    //     
    //    L2Effect[] effects = target.getEffectList().getAllFirstEffects(); 
    //    Arrays.sort(effects, EffectsComparator.getInstance()); 
    //     
    //    for(L2Effect effect : effects) 
    //        if(effect != null && effect.isInUse()) 
    //            effect.addIcon(this); 
    //} 
    //
    //@Override 
    //protected void writeImpl()  
    //{ 
    //    writeEx(0xE5); 
    //    writeD(objId); 
    //    writeH(_effects.size()); 
    //    for (IconEffect e : _effects) 
    //    { 
    //        writeD(e.getSkillId()); 
    //        writeH(e.getLevel()); 
    //        writeD(0x00); 
    //        writeD(e.getDuration()); 
    //        writeD(0x00); 
    //    } 
    //} 
    //
    //@Override 
    //public void addIconEffect(int skillId, int level, int duration)  
    //{ 
    //    _effects.add(new IconEffect(skillId, level, duration));         
    //} 
//}  