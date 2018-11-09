package structures;

import java.util.*;


public class Tree {
	
	TagNode root=null;
	Scanner sc;

	public Tree(Scanner sc) {
		this.sc = sc;
		root = null;
	}
	
//GRADED METHODS 
	
	public void build() 
	{
		// IMPORTANT - WHEN TRAVERSING, TREAT CHILD AS LEFT & SIBLING AS RIGHT 
		Stack<TagNode> htmlTags = new Stack<TagNode>();
		root = new TagNode( "html" , null, null );
		TagNode firstbranch = new TagNode ( "body", null, null );
		root.firstChild = firstbranch;
		
		htmlTags.push( root );
		htmlTags.push( firstbranch );
		
		Scanner tempsc = sc;
		tempsc.nextLine();
		String line = tempsc.nextLine();
		
		while ( !line.equals( "</body>" ) )
		{
			line = tempsc.nextLine();
			if ( isTag(line) )
			{
				if ( line.indexOf('/') != -1 ) // str.indexOf('/') == -1 )
				{
					htmlTags.pop();
					continue;
				}
				else
				{	
					TagNode temp = new TagNode ( line.substring(1, line.length() - 1) , null, null );
					if ( htmlTags.peek().firstChild == null )
					{
						htmlTags.peek().firstChild = temp;
						htmlTags.push(temp);
						continue;
					}
					else
					{
						TagNode tempSibling = htmlTags.peek().firstChild ;
						while ( tempSibling.sibling != null )
						{
							tempSibling = tempSibling.sibling;
						}
						tempSibling.sibling = temp;
						htmlTags.push(temp);
						continue;
					}
				}
			}
			else
			{
				TagNode temp = new TagNode( line , null, null );
				if ( htmlTags.peek().firstChild == null )
					htmlTags.peek().firstChild = temp;
				else
				{
					TagNode tempSibling = htmlTags.peek().firstChild;
					while ( tempSibling.sibling != null )
					{
						tempSibling = tempSibling.sibling;
					}
					tempSibling.sibling = temp;
				}
			}
		}
	}
	
	
	public void replaceTag(String oldTag, String newTag) 
	{
		// IMPORTANT - WHEN TRAVERSING, TREAT CHILD AS LEFT & SIBLING AS RIGHT 
		replaceTag( root.firstChild, oldTag, newTag );
	}
	

	public void boldRow(int row) 
	{
		// IMPORTANT - WHEN TRAVERSING, TREAT CHILD AS LEFT & SIBLING AS RIGHT 
		boldRow ( traverseSearch( root.firstChild, "table" ).firstChild , row );
	}
	

	public void removeTag(String tag) 
	{
		// IMPORTANT - WHEN TRAVERSING, TREAT CHILD AS LEFT & SIBLING AS RIGHT 
		//Case 1: p, em, b tags 
		//Case 2: ol, ul tags 
		
		 while ( traverseSearch( root, tag ) != null )
		{
			TagNode temp = traverseSearch ( root, tag );
			TagNode parentPtr = getParent( root, temp );
			
			if ( temp == parentPtr.firstChild && temp.sibling == null ) // tag is only child of parent 
			{
				if ( tag.equals("ol") || tag.equals("ul") )
					listsToParagraphs( temp.firstChild );
				parentPtr.firstChild = temp.firstChild; 
			}
			else if ( temp == parentPtr.firstChild && temp.sibling != null ) // tag is eldest child of parent 
			{ 	
				if ( tag.equals("ol") || tag.equals("ul") )
					listsToParagraphs( temp.firstChild );
				
				TagNode tempsKid = temp.firstChild;
				TagNode unclePtr = temp;

				while ( tempsKid.sibling != null )
					tempsKid = tempsKid.sibling;
				while ( unclePtr.sibling != null )
				{
					tempsKid.sibling = unclePtr;
					unclePtr = unclePtr.sibling;
					tempsKid.sibling = tempsKid.sibling.sibling;
				}
				
				parentPtr.firstChild = temp.firstChild;
			} 
			else if ( getSiblings( parentPtr.firstChild ).contains ( temp ) ) // tag is not the eldest child
			{
				if ( tag.equals("ol") || tag.equals("ul") )
					listsToParagraphs( temp.firstChild );
				
				TagNode tempsKid = temp.firstChild;
				TagNode unclePtr = temp;
				TagNode eldestUncle = parentPtr.firstChild;
				
				while ( tempsKid.sibling != null )
					tempsKid = tempsKid.sibling;
				while ( unclePtr.sibling != null )
				{
					tempsKid.sibling = unclePtr;
					unclePtr = unclePtr.sibling;
					tempsKid.sibling = tempsKid.sibling.sibling;
				}
				
				while ( eldestUncle.sibling != temp )
					eldestUncle = eldestUncle.sibling;
				
				eldestUncle.sibling = temp.firstChild;
			}
			else
				continue;
		}  
	}
	

	public void addTag(String word, String tag) 
	{
		// IMPORTANT - WHEN TRAVERSING, TREAT CHILD AS LEFT & SIBLING AS RIGHT 
		
		addTagRecursive( root, word, tag );
		
	}
	

	public String getHTML() {
		StringBuilder sb = new StringBuilder();
		getHTML(root, sb);
		return sb.toString();
	}
	
	private void getHTML(TagNode root, StringBuilder sb) {
		for (TagNode ptr=root; ptr != null;ptr=ptr.sibling) {
			if (ptr.firstChild == null) {
				sb.append(ptr.tag);
				sb.append("\n");
			} else {
				sb.append("<");
				sb.append(ptr.tag);
				sb.append(">\n");
				getHTML(ptr.firstChild, sb);
				sb.append("</");
				sb.append(ptr.tag);
				sb.append(">\n");	
			}
		}
	}
	
	public void print() {
		print(root, 1);
	}
	
	private void print(TagNode root, int level) {
		for (TagNode ptr=root; ptr != null;ptr=ptr.sibling) {
			for (int i=0; i < level-1; i++) {
				System.out.print("      ");
			};
			if (root != this.root) {
				System.out.print("|---- ");
			} else {
				System.out.print("      ");
			}
			System.out.println(ptr.tag);
			if (ptr.firstChild != null) {
				print(ptr.firstChild, level+1);
			}
		}
	}

//GRADED METHODS END

//HELPER METHODS
	private static boolean isTag( String str )
	{
		if( str.indexOf('<') != -1 )
			return true;
		else
			return false;
	}
	private void replaceTag( TagNode node, String oldTag, String newTag )
	{
		if ( node == null )
			return;
		else
		{
			if ( node.tag.equals( oldTag ) )
				node.tag = newTag;
			replaceTag( node.firstChild, oldTag, newTag );
			replaceTag( node.sibling, oldTag, newTag );
		}
	}
	private void boldRow ( TagNode node, int r )
	{
		r = r - 1;
		
		while ( r != 0 )
		{
			node = node.sibling;
			r--;
		}
		
		TagNode ptr = node.firstChild;
		
		while ( ptr != null )
		{
			TagNode temp = ptr.firstChild;
			TagNode boldNode = new TagNode ( "b" ,temp , null );
			ptr.firstChild = boldNode;
			ptr = ptr.sibling;
		}
	}
	private TagNode traverseSearch ( TagNode rootNode, String targetTag )
	{
		if ( rootNode != null )
		{
			if ( rootNode.tag.equals(targetTag) )
				return rootNode;
			else
			{	
				TagNode ptr = traverseSearch( rootNode.firstChild , targetTag );
				if ( ptr == null )
					ptr = traverseSearch( rootNode.sibling, targetTag );
				return ptr;
			}
		}
		else
			return null;

	}
/*	private TagNode traverse ( TagNode rootNode )
	{
		if ( rootNode != null )
	{
			TagNode ptr = traverse( rootNode.firstChild );
			if ( ptr == null )
				ptr = traverse( rootNode.sibling );
			return ptr;
		}
		else
			return null;
	}*/
	private TagNode getParent ( TagNode rootNode , TagNode child )
	{
		if ( traverseSearch( root, child.tag ) == null || rootNode == null )
			return null;
		else
		{
			if ( rootNode.firstChild == child || getSiblings( rootNode.firstChild ).contains( child ) )
				return rootNode;
			else
			{
				TagNode ptr = getParent ( rootNode.firstChild, child );
				if ( ptr == null )
					ptr = getParent ( rootNode.sibling, child );
				return ptr;
			}
		}
	}
	private ArrayList<TagNode> getSiblings ( TagNode eldest )
	{
		ArrayList<TagNode> siblings = new ArrayList<TagNode>();
		TagNode ptr = eldest;
		
		while ( ptr != null )
		{	
			siblings.add( ptr );
			ptr = ptr.sibling;
		}
		
		return siblings;
		
	}
	private void listsToParagraphs ( TagNode fC )
	{
		if ( fC == null )
			return;
		if ( fC.tag.equals("li") )
			fC.tag = "p";
		
		listsToParagraphs( fC.sibling );

	}
	private boolean isPunct( char c )
	{
		if ( c == '!' || c == ',' || c == '.'  || c == '?' || c == ';' || c == ':' )
			return true;
		else
			return false;
	}
	private boolean isTextTag( TagNode node )
	{
		if ( node.tag == "em" || node.tag == "b" || node.tag == "body" || node.tag == "ol" 
				|| node.tag == "ul" || node.tag == "li" || node.tag == "p" || node.tag == "table" 
				|| node.tag == "td" || node.tag == "tr" || node.tag == "html" )
			return false;
		else
			return true;
	}
	private void addTagRecursive ( TagNode rootNode, String word, String tag )
	{
		if ( rootNode != null )
		{
			String str = rootNode.tag;
			int wordIndex = str.indexOf(word);
			if ( isTextTag( rootNode ) && wordIndex != -1 )
			{
				TagNode pops = getParent( root, rootNode );
				TagNode temp = rootNode;
			
				// String tempStr = str.trim();
				String[] temps = str.split("((?<=(?i)" + word + ")|(?=(?i)" + word + "))" );
						
				ArrayList<String> temps2 = new ArrayList<String> ( Arrays.asList( temps) );

				String[] newNodes = temps2.toArray( new String[temps2.size()] );
				
				TagNode placeHolder = new TagNode( "newNode", null, null );
				TagNode ptr = placeHolder;
				
				for ( int i = 0; i < newNodes.length; i++ )
				{
					
					if ( newNodes[i] == "" )
						continue;
					
					if ( newNodes[i].indexOf(word) != -1 )
					{
						while ( ptr.sibling != null )
						{
							ptr = ptr.sibling;
						}
						
						TagNode 	ogWord = new TagNode ( newNodes[i], null, null );
						ptr.sibling = new TagNode( tag, ogWord, null );
					}
					else
					{
						while ( ptr.sibling != null )
						{
							ptr = ptr.sibling;
						}
						
						ptr.sibling = new TagNode( newNodes[i], null, null );
					}
				}
				
				if ( rootNode.sibling != null && rootNode == pops.firstChild )
				{
					while ( temp.sibling != null )
					{
						ptr.sibling.sibling = temp.sibling;
						temp = temp.sibling;
						ptr = ptr.sibling;
					}
					
					pops.firstChild = placeHolder.sibling;
				}
				
				else if ( rootNode.sibling == null && rootNode == pops.firstChild )
					pops.firstChild = placeHolder.sibling;
				
				else
				{
					TagNode ptr2 = pops.firstChild;
					
					while ( temp.sibling != null )
					{
						ptr.sibling.sibling = temp.sibling;
						temp = temp.sibling;
						ptr = ptr.sibling;
					}
					
					while ( ptr2.sibling != temp )
						ptr2 = ptr2.sibling;
					
					ptr2 = placeHolder.sibling;
				}

			}

			addTagRecursive( rootNode.firstChild, word, tag );
			addTagRecursive( rootNode.sibling, word, tag );
		}
		else
			return;
	}
//HELPER METHODS END
}