package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.extensions.scripts.Scripts.ScriptClassAndMethod;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.playerSubOrders.BypassEngine;
import ru.l2gw.util.Files;
import ru.l2gw.util.Strings;

import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * the HTML parser in the client knowns these standard and non-standard tags and attributes
 * VOLUMN
 * UNKNOWN
 * UL
 * U
 * TT
 * TR
 * TITLE
 * TEXTCODE
 * TEXTAREA
 * TD
 * TABLE
 * SUP
 * SUB
 * STRIKE
 * SPIN
 * SELECT
 * RIGHT
 * PRE
 * P
 * OPTION
 * OL
 * MULTIEDIT
 * LI
 * LEFT
 * INPUT
 * IMG
 * I
 * HTML
 * H7
 * H6
 * H5
 * H4
 * H3
 * H2
 * H1
 * FONT
 * EXTEND
 * EDIT
 * COMMENT
 * COMBOBOX
 * CENTER
 * BUTTON
 * BR
 * BODY
 * BAR
 * ADDRESS
 * A
 * SEL
 * LIST
 * VAR
 * FORE
 * READONL
 * ROWS
 * VALIGN
 * FIXWIDTH
 * BORDERCOLORLI
 * BORDERCOLORDA
 * BORDERCOLOR
 * BORDER
 * BGCOLOR
 * BACKGROUND
 * ALIGN
 * VALU
 * READONLY
 * MULTIPLE
 * SELECTED
 * TYP
 * TYPE
 * MAXLENGTH
 * CHECKED
 * SRC
 * Y
 * X
 * QUERYDELAY
 * NOSCROLLBAR
 * IMGSRC
 * B
 * FG
 * SIZE
 * FACE
 * COLOR
 * DEFFON
 * DEFFIXEDFONT
 * WIDTH
 * VALUE
 * TOOLTIP
 * NAME
 * MIN
 * MAX
 * HEIGHT
 * DISABLED
 * ALIGN
 * MSG
 * LINK
 * HREF
 * ACTION
 */
public class NpcHtmlMessage extends L2GameServerPacket
{
	// d S
	// d is usually 0, S is the html text starting with <html> and ending with </html>
	//
	private int _npcObjId, _questId;
	private String _html;
	private String _file = null;
	private ArrayList<String> replaces = new ArrayList<String>();
	private int item_id = 0;
	private boolean can_writeImpl = false;
	private boolean have_appends = false;

	public NpcHtmlMessage(L2Player player, L2NpcInstance npc, String filename, Integer val)
	{
		_npcObjId = npc.getObjectId();

		player.setLastNpc(npc);
		setFile(filename);

		// Добавить в конец странички текст, определенный в скриптах.
		ArrayList<ScriptClassAndMethod> appends = Scripts.dialogAppends.get(npc.getNpcId());
		if(appends != null && appends.size() > 0)
		{
			have_appends = true;
			if(filename != null && filename.equalsIgnoreCase("data/html/npcdefault.htm"))
				setHtml(""); // контент задается скриптами через DialogAppend_
			else
				setFile(filename);

			String replaces = "";

			// Добавить в конец странички текст, определенный в скриптах.
			Object[] script_args = new Object[] { new Integer(val) };
			for(ScriptClassAndMethod append : appends)
			{
				Object obj = player.callScripts(append.scriptClass, append.method, script_args);
				if(obj != null)
					replaces += obj;
			}
			
			if(!replaces.equals(""))
			{
				replace("</body>", "\n" + Strings.bbParse(replaces) + "</body>");
				replace("</BODY>", "\n" + Strings.bbParse(replaces) + "</BODY>");
			}
		}
		else
			setFile(filename);

		replace("%npcId%", String.valueOf(npc.getNpcId()));
		replace("%playername%", String.valueOf(player.getName()));
		replace("%npcname%", npc.getName());
	}

	public NpcHtmlMessage(L2Player player, L2NpcInstance npc)
	{
		_npcObjId = npc.getObjectId();
		player.setLastNpc(npc);
	}

	/**
	 * ONLY FOR NON NPC Messages
	 * @param AbstractnpcObjId
	 */
	public NpcHtmlMessage(int npcObjId)
	{
		_npcObjId = npcObjId;
	}

	public final NpcHtmlMessage setHtml(String text)
	{
		if(!text.contains("<html>"))
			text = "<html><body>" + text + "</body></html>"; //<title>Message:</title> <br><br><br>
		_html = text;
		return this;
	}

	public final NpcHtmlMessage setFile(String file)
	{
		_file = file;
		return this;
	}

	public final NpcHtmlMessage setItemId(int _item_id)
	{
		item_id = _item_id;
		return this;
	}

	public void setQuest(int quest)
	{
		_questId = quest;
	}

	private void setFile()
	{
		String content = loadHtml(_file, getClient().getPlayer());
		if(content == null)
			setHtml(have_appends && _file.endsWith(".htm") ? "" : _file);
		else
			setHtml(content);
	}

	protected String loadHtml(String name, L2Player player)
	{
		return Files.read(name, player);
	}

	protected String html_load(String name, String lang)
	{
		String content = Files.read(name, lang);
		if(content == null)
			content = "Can't find file'" + name + "'";
		return content;
	}

	public void replace(String pattern, String value)
	{
		replaces.add(pattern);
		replaces.add(value);
	}

	@Override
	final public void runImpl()
	{
		if(_file != null)
			setFile();
		L2Player player = getClient().getPlayer();
		if(player == null || _html == null)
			return;

		for(int i = 0; i < replaces.size(); i += 2)
		{
			_html = _html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));
			_html = _html.replace("\\$", "$");
		}

		_html = _html.replaceAll("%objectId%", String.valueOf(_npcObjId));

		BypassEngine.cleanBypasses(false, player);
		_html = BypassEngine.encodeBypasses(_html, false, player);

		if(player.isGM())
			player.sendPacket(new Say2(0, Say2C.ALL, "HTML", _file == null ? "Generated on the fly HTM" : _file.replace("data/html/", "")));

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		if(_questId > 0)
		{
			writeC(EXTENDED_PACKET);
			writeH(0x8E);
			writeD(_npcObjId);
			writeS(_html);
			writeD(_questId);
		}
		else
		{
			writeC(0x19);
			writeD(_npcObjId);
			writeS(_html);
			writeD(item_id);
			writeD(0x00); //???
		}
	}
}