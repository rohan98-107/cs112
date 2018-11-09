package apps;

import structures.*;
import structures.Vertex.Neighbor;

import java.util.ArrayList;

public class MST {
	
	/**
	 * Initializes the algorithm by building single-vertex partial trees
	 * 
	 * @param graph Graph for which the MST is to be found
	 * @return The initial partial tree list
	 */
	public static PartialTreeList initialize(Graph graph) {
	
		PartialTreeList L = new PartialTreeList(); //Create an empty list L of partial trees
		int num_vertices = graph.vertices.length;
		
		for ( int i = 0; i < num_vertices; i++ )
		{
			PartialTree T = new PartialTree( graph.vertices[i] ); //Create a partial tree T containing only v // Mark v as belonging to T
			MinHeap<PartialTree.Arc> P = T.getArcs(); //Create a priority queue (heap) P and associate it with T
			
			Neighbor ptr = graph.vertices[i].neighbors;
			 //Insert all of the arcs connected to v into P
			 while ( ptr != null )
			{
				PartialTree.Arc line = new PartialTree.Arc( graph.vertices[i], ptr.vertex, ptr.weight );
				P.insert(line);
				ptr = ptr.next;
			}
			
			L.append(T); // Add the partial tree T to list L
		}
		return L;
	}

	/**
	 * Executes the algorithm on a graph, starting with the initial partial tree list
	 * 
	 * @param ptlist Initial partial tree list
	 * @return Array list of all arcs that are in the MST - sequence of arcs is irrelevant
	 */
	public static ArrayList<PartialTree.Arc> execute(PartialTreeList ptlist) 
	{
		ArrayList<PartialTree.Arc> result = new ArrayList<PartialTree.Arc>();
		// ArrayList<PartialTree> convenienceList = PTlist_to_ArrayList( ptlist );
		
		if ( ptlist.size() == 1 )
			result.add( ptlist.remove().getArcs().getMin() );
			
		while ( ptlist.size() > 1 )
		{
			PartialTree PTX = ptlist.remove(); // Remove the first partial tree PTX from L
			MinHeap<PartialTree.Arc> PQX = PTX.getArcs(); // Let PQX be PTX's priority queue
			PartialTree.Arc alpha = PQX.deleteMin(); // Remove highest priority arc from PQX, say this arc is alpha
			
			// PartialTreeList tempList = ptlist;
			while( alpha.v1.getRoot() == alpha.v2.getRoot() ) // if v2 also belongs to PTX, remove another arc (let PQX = next highest priority)
				alpha = PQX.deleteMin();
			
			result.add( alpha ); // Report alpha 
			
			PartialTree PTY = ptlist.removeTreeContaining( alpha.v2 ); // Find partial tree PTY to which v2 belongs, remove PTY from L
			// MinHeap<PartialTree.Arc> PQY = PTY.getArcs(); //Let PQY be PTY's priority queue 
			
			PTX.merge( PTY ); // combine PTX and PTY - automatically merges PQX and PQY -> PQX 
			
			ptlist.append( PTX ); // Append resulting tree to the end of L
		}
		
		return result;
	}
	
	/*private static ArrayList<PartialTree> PTlist_to_ArrayList( PartialTreeList ptlist )
	{
		PartialTreeList temp = ptlist;
		ArrayList<PartialTree> res = new ArrayList<PartialTree>();
		
		while ( temp.size() != 0)
		{
			res.add(temp.remove());
		}
		
		return res;
	}*/
}
