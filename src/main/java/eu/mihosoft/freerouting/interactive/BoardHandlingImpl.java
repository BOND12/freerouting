package eu.mihosoft.freerouting.interactive;

import java.io.*;

import eu.mihosoft.freerouting.board.Communication;
import eu.mihosoft.freerouting.board.LayerStructure;
import eu.mihosoft.freerouting.board.RoutingBoard;
import eu.mihosoft.freerouting.board.TestLevel;
import eu.mihosoft.freerouting.geometry.planar.IntBox;
import eu.mihosoft.freerouting.geometry.planar.PolylineShape;
import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.rules.BoardRules;

import java.util.Locale;

/**
 * Base implementation for headless mode
 *
 * Andrey Belomutskiy
 * 6/28/2014
 */
public class BoardHandlingImpl implements IBoardHandling {
    /**
     * The file used for logging interactive action,
     * so that they can be replayed later
     */
    public final ActivityReplayFile activityReplayFile = new ActivityReplayFile();
    /** The current settings for interactive actions on the board */
    public Settings settings = null;
    /** The board database used in this interactive handling. */
    protected RoutingBoard board = null;
    
    private byte[] serializedBoard = null;

    public BoardHandlingImpl() {
    }

    /**
     * Gets the routing board of this board handling.
     */
    @Override
    public RoutingBoard get_routing_board()
    {
        return this.board;
    }
    
    public synchronized void update_routing_board(RoutingBoard routing_board) {
    	this.board = routing_board;
    	serializedBoard = null;
    }

    public synchronized RoutingBoard deep_copy_routing_board()
    {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        
        try
        {
           if (serializedBoard == null) // cache the board byte array
           {
	           ByteArrayOutputStream bos =  new ByteArrayOutputStream(); 
	           oos = new ObjectOutputStream(bos); 
	           	           
	           oos.writeObject(this.board);   // serialize this.board
	           oos.flush(); 
	           
	           serializedBoard = bos.toByteArray();
           }
           
           ByteArrayInputStream bin = new ByteArrayInputStream(serializedBoard); 
           ois = new ObjectInputStream(bin);   

           RoutingBoard board_copy =  (RoutingBoard)ois.readObject(); 
           
           board_copy.set_test_level(this.board.get_test_level());  //test_level is transient
           
           //board_copy.clear_autoroute_database();
           board_copy.clear_all_item_temporary_autoroute_data();
           board_copy.finish_autoroute(); 
           
           return board_copy;
        }
        catch(Exception e)
        {
           System.err.println("Exception in deep_copy_routing_board = " + e);
           return null;
        }
        finally
        {
        	try 
        	{
	           if (oos != null) oos.close();
	           if (ois != null) ois.close();
        	}
        	catch(Exception e) {}
        }
    }

    @Override
    public Settings get_settings() {
        return settings;
    }

    /**
     * Initializes the manual trace widths from the default trace widths in the board rules.
     */
    @Override
    public void initialize_manual_trace_half_widths()
    {
        for (int i = 0; i < settings.manual_trace_half_width_arr.length; ++i)
        {
            settings.manual_trace_half_width_arr[i] = this.board.rules.get_default_net_class().get_trace_half_width(i);
        }
    }

    @Override
    public void create_board(IntBox p_bounding_box, LayerStructure p_layer_structure, PolylineShape[] p_outline_shapes, String p_outline_clearance_class_name, BoardRules p_rules, Communication p_board_communication, TestLevel p_test_level) {
        if (this.board != null)
        {
            FRLogger.warn(" BoardHandling.create_board: eu.mihosoft.freerouting.board already created");
        }
        int outline_cl_class_no = 0;

        if (p_rules != null)
        {
            if (p_outline_clearance_class_name != null && p_rules.clearance_matrix != null)
            {
                outline_cl_class_no = p_rules.clearance_matrix.get_no(p_outline_clearance_class_name);
                outline_cl_class_no = Math.max(outline_cl_class_no, 0);
            }
            else
            {
                outline_cl_class_no =
                        p_rules.get_default_net_class().default_item_clearance_classes.get(eu.mihosoft.freerouting.rules.DefaultItemClearanceClasses.ItemClass.AREA);
            }
        }
        this.board =
                new RoutingBoard(p_bounding_box, p_layer_structure, p_outline_shapes, outline_cl_class_no,
                        p_rules, p_board_communication, p_test_level);

        this.settings = new Settings(this.board, this.activityReplayFile);
    }

    @Override
    public Locale get_locale() {
        return java.util.Locale.ENGLISH;
    }
}
