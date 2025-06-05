import UsersTable from "../../components/UserTable/UsersTable"
import SearchBar from "../../components/SearchBar"
export default function AllUsers(){

    return(
        <div className="flex flex-col gap-4">
            <h1 className="text-5xl font-bold text-primary p-2">Users</h1>
            <SearchBar/>
            <UsersTable/>
        </div>
    )  
}