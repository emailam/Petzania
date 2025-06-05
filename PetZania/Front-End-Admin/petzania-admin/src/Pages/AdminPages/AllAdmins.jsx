import SearchBar from "../../components/SearchBar"
import AdminsTable from "../../components/AdminTable/AdminsTable"
export default function AllAdmins(){

    return(
        <div className="flex flex-col gap-4 ">
            <h1 className="text-5xl font-bold text-primary ">Admins</h1>
            <SearchBar/>
            <AdminsTable/>
        </div>
    )  
}