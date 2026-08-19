import sys
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # 1. Add imports
    if 'org.hibernate.annotations.Filter' not in content:
        import_stmt = "import org.hibernate.annotations.Filter;\nimport org.hibernate.annotations.FilterDef;\nimport org.hibernate.annotations.ParamDef;\n"
        content = re.sub(r'(import [^;]+;\n)+', lambda m: m.group(0) + import_stmt, content, count=1)

    # 2. Add Filter annotations
    if '@FilterDef' not in content:
        entity_idx = content.find('public class')
        if entity_idx != -1:
            annotation = "@FilterDef(name = \"deletedFilter\", defaultCondition = \"deleted = false\")\n@Filter(name = \"deletedFilter\")\n"
            content = content[:entity_idx] + annotation + content[entity_idx:]

    # 3. Add implements SoftDeletable
    if 'implements SoftDeletable' not in content:
        content = re.sub(r'(public class [a-zA-Z0-9_]+)(\s*\{|\s+implements\s+[a-zA-Z0-9_, \n]+(?:\{|\s))',
                         lambda m: m.group(1) + (" implements SoftDeletable" if '{' in m.group(2) else " implements SoftDeletable,") + m.group(2).replace("implements ", ""), content, count=1)

    # 4. Add fields
    if 'private boolean deleted' not in content:
        fields = """
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by")
    private String deletedBy;
"""
        # Find first field declaration or constructor
        idx = content.find('{', content.find('public class'))
        content = content[:idx+1] + fields + content[idx+1:]

    # 5. Add getters and setters
    if 'public boolean isDeleted()' not in content:
        methods = """
    @Override
    public boolean isDeleted() { return deleted; }
    @Override
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    @Override
    public LocalDateTime getDeletedAt() { return deletedAt; }
    @Override
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public LocalDateTime getPermanentDeleteAt() { return permanentDeleteAt; }
    @Override
    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) { this.permanentDeleteAt = permanentDeleteAt; }

    @Override
    public String getDeletedBy() { return deletedBy; }
    @Override
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
"""
        # insert before the final closing brace of the class (or before Builder)
        idx = content.rfind('public static Builder builder()')
        if idx == -1:
            idx = content.rfind('}')
            content = content[:idx] + methods + content[idx:]
        else:
            content = content[:idx] + methods + "\n" + content[idx:]

    # 6. Add builder methods
    if 'public static class Builder' in content and 'public Builder deleted(boolean v)' not in content:
        builder_methods = """
        public Builder deleted(boolean v) {
            instance.deleted = v;
            return this;
        }
        public Builder deletedAt(LocalDateTime v) {
            instance.deletedAt = v;
            return this;
        }
        public Builder permanentDeleteAt(LocalDateTime v) {
            instance.permanentDeleteAt = v;
            return this;
        }
        public Builder deletedBy(String v) {
            instance.deletedBy = v;
            return this;
        }
"""
        # Need to determine what the instance variable is called (e.g., user, a, etc.)
        match = re.search(r'private final [A-Za-z]+ ([a-zA-Z0-9_]+) = new [A-Za-z]+\(\);', content)
        if match:
            var_name = match.group(1)
            builder_methods = builder_methods.replace('instance.', f'{var_name}.')
            
            idx = content.rfind('public ', content.find('public static class Builder'), content.rfind('build()'))
            content = content[:idx] + builder_methods + content[idx:]

    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    for f in sys.argv[1:]:
        process_file(f)
