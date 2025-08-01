package cn.nukkit.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class ListTag<T extends Tag> extends Tag {

    private List<T> list = new ArrayList<>();

    public byte type;

    public ListTag() {
        super("");
    }

    public ListTag(String name) {
        super(name);
    }

    public ListTag(List<T> list) {
        this("", list);
    }

    public ListTag(String name, List<T> list) {
        super(name);
        this.list = list;
    }

    public ListTag(int type, Collection<T> tags) {
        this.type = (byte) type;
        this.list.addAll(tags);
    }

    @Override
    void write(NBTOutputStream dos) throws IOException {
        if (!list.isEmpty()) type = list.get(0).getId();
        else type = 1;

        dos.writeByte(type);
        dos.writeInt(list.size());
        for (T aList : list) aList.write(dos);
    }

    @Override
    @SuppressWarnings("unchecked")
    void load(NBTInputStream dis) throws IOException {
        type = dis.readByte();
        int size = dis.readInt();

        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Tag tag = Tag.newTag(type, null);
            tag.load(dis);
            tag.setName("");
            list.add((T) tag);
        }
    }

    @Override
    public byte getId() {
        return TAG_List;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",\n\t");
        list.forEach(tag -> joiner.add(tag.toString().replace("\n", "\n\t")));
        return "ListTag '" + this.getName() + "' (" + list.size() + " entries of type " + Tag.getTagName(type) + ") {\n\t" + joiner.toString() + "\n}";
    }

    @Override
    public String toSNBT() {
        return "[" + list.stream()
                .map(Tag::toSNBT)
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public String toSNBT(int space) {
        StringBuilder addSpace = new StringBuilder();
        addSpace.append(" ".repeat(Math.max(0, space)));
        if (list.isEmpty()) {
            return "[]";
        } else if (list.get(0) instanceof StringTag || list.get(0) instanceof CompoundTag || list.get(0) instanceof ListTag<?>) {
            StringJoiner joiner1 = new StringJoiner(",\n" + addSpace);
            list.forEach(tag -> joiner1.add(tag.toSNBT(space).replace("\n", "\n" + addSpace)));
            return "[\n" + addSpace + joiner1 + "\n]";
        } else {
            StringJoiner joiner2 = new StringJoiner(", ");
            list.forEach(tag -> joiner2.add(tag.toSNBT(space)));
            return "[" + joiner2 + "]";
        }
    }

    @Override
    public void print(String prefix, PrintStream out) {
        super.print(prefix, out);

        out.println(prefix + '{');
        String orgPrefix = prefix;
        prefix += "   ";
        for (T aList : list) aList.print(prefix, out);
        out.println(orgPrefix + '}');
    }

    public ListTag<T> add(T tag) {
        type = tag.getId();
        tag.setName("");
        list.add(tag);
        return this;
    }

    public ListTag<T> add(int index, T tag) {
        type = tag.getId();
        tag.setName("");

        if (index >= list.size()) {
            list.add(index, tag);
        } else {
            list.set(index, tag);
        }
        return this;
    }

    @Override
    public List<Object> parseValue() {
        List<Object> value = new ArrayList<>(this.list.size());

        for (T t : this.list) {
            value.add(t.parseValue());
        }

        return value;
    }

    public T get(int index) {
        return list.get(index);
    }

    public List<T> getAll() {
        return new ArrayList<>(list);
    }

    public List<T> getAllUnsafe() {
        return list;
    }

    public void setAll(List<T> tags) {
        this.list = new ArrayList<>(tags);
    }

    public void remove(T tag) {
        list.remove(tag);
    }

    public void remove(int index) {
        list.remove(index);
    }

    public void removeAll(Collection<T> tags) {
        list.removeAll(tags);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(T tag) {
        return list.contains(tag);
    }

    @Override
    public Tag copy() {
        ListTag<T> res = new ListTag<>(getName());
        res.type = type;
        for (T t : list) {
            @SuppressWarnings("unchecked")
            T copy = (T) t.copy();
            res.list.add(copy);
        }
        return res;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            ListTag o = (ListTag) obj;
            if (type == o.type) {
                return list.equals(o.list);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, list);
    }
}
