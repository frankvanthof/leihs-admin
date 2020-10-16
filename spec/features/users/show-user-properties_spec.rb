require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end

      @user = @users.sample

      sign_in_as @admin
    end


    scenario 'showing user properties on the user show page' do

      visit '/admin/'
      click_on 'Users'

      fill_in 'search', with: \
        "#{@user.firstname} #{@user.lastname}"

      click_on_first "#{@user.firstname} #{@user.lastname}"

      wait_until(10) do
        page.has_content? "User #{@user.firstname} #{@user.lastname}"
      end

      expect(page).to have_content 'firstname'
      expect(page).to have_content 'lastname'
      expect(page).to have_content 'org_id'
      expect(page).to have_content 'badge_id'
      expect(page).not_to have_content /\s+password\s+/

    end
  end
end
