import { Menu, type MenuProps } from "antd";
import favicon from "@/assets/favicon.ico";

type MenuItem = Required<MenuProps>["items"][number];

const menuItems: MenuItem[] = [
    {
        label: "Mojito",
        key: "home",
        icon: <img src={favicon} alt="favicon" width={16} height={16} />,
    },
];

const Navbar: React.FC = () => {
    return <Menu items={menuItems} mode="horizontal" />;
};

export default Navbar;
