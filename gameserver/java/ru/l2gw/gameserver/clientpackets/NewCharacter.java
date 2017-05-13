package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.serverpackets.NewCharacterSuccess;
import ru.l2gw.gameserver.tables.CharTemplateTable;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class NewCharacter extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		//sendPacket(new NewCharacterSuccess());
		if(Config.DEBUG)
			_log.warn("CreateNewChar");
        
		NewCharacterSuccess ct = new NewCharacterSuccess();
        
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.fighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.mage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.dwarvenFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.maleSoldier, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.femaleSoldier, true));
        
		sendPacket(ct);
	}
}
