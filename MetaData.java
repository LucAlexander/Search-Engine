
public class MetaData{
	private String description, keywords;
	MetaData(String k, String d){
		this.keywords = k;
		this.description = d;
	}
	public String getKeywords(){
		return this.keywords;
	}
	public String getDescription(){
		return this.description;
	}
	public void setKeywords(String k){
		this.keywords = k;
	}
	public void setDescription(String d){
		this.description = d;
	}
}

