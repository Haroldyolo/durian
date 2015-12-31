/*
 * Copyright (C) 2015 thirdy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package qic.ui.extra;

import java.awt.Component;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import qic.util.ImageCache;

/**
 * @author thirdy
 *
 */
public class ArtColumnRenderer extends DefaultTableCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	private ImageIcon defaultImage;

	public ArtColumnRenderer() {
		try {
			this.defaultImage = new ImageIcon(ImageIO.read(new File("images/default.png")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		setHorizontalAlignment(JLabel.CENTER);
	}
	
	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ImageIcon image = defaultImage;
        
        Image img = ImageCache.getInstance().get(value.toString());
        if (img != null) {
        	image = new ImageIcon(img);
		}
        setIcon(image);
        setText("");
        
        table.setRowHeight(row, image.getIconHeight());
        return this;
    }

}
