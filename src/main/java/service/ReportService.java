package service;

import dao.ReportDAO;
import model.Admin;
import model.Farmer;
import model.ReportPeriod;
import model.Reports.CategorySales;
import model.Reports.FarmerSales;
import model.Reports.ProductSales;
import model.Reports.SaleLine;
import model.Reports.SalesSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * Sales reports. Farmers only ever see their own figures; the
 * marketplace-wide reports need an Admin.
 */
public class ReportService {

    /** Everything on a farmer's Sales page, loaded together. */
    public record FarmerReport(SalesSummary summary, List<ProductSales> products, List<SaleLine> history) {
    }

    /** Everything on the admin Reports page. */
    public record MarketReport(List<FarmerSales> farmers, List<ProductSales> products, List<CategorySales> categories) {
    }

    private final ReportDAO reportDAO = new ReportDAO();

    public FarmerReport farmerReport(Farmer farmer, ReportPeriod period) {
        LocalDate start = period.startDate(LocalDate.now());
        int id = farmer.getUserId();
        return new FarmerReport(reportDAO.farmerSummary(id, start),
                reportDAO.farmerProducts(id, start),
                reportDAO.farmerSaleLines(id, start));
    }

    public MarketReport marketReport(Admin admin, ReportPeriod period) {
        LocalDate start = period.startDate(LocalDate.now());
        return new MarketReport(reportDAO.farmers(start), reportDAO.allProducts(start), reportDAO.categories(start));
    }
}
