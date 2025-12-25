public class Role {
    private String name;
    private Integer id;

    public Role(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public String getName() { return name; }
    public Integer getId() { return id; }

    @Override
    public String toString() {
        return "Role{name='" + name + "', id=" + id + "}";
    }
}
